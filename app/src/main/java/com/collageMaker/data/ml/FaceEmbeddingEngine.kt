package com.collageMaker.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import androidx.core.graphics.scale

class FaceEmbeddingEngine(private val context: Context? = null) {

    private var tfliteInterpreter: Interpreter? = null
    private val embeddingDim = 128
    private val modelInputSize = 112

    init {
        tryLoadModel()
    }

    private fun tryLoadModel() {
        if (context == null) return
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd("models/mobilefacenet.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            tfliteInterpreter = Interpreter(mappedByteBuffer)
            Log.i(TAG, "MobileFaceNet TFLite model loaded successfully.")
        } catch (e: Exception) {
            tfliteInterpreter = null
            Log.w(TAG, "TFLite model unavailable — using enhanced spatial fallback embedding. Reason: ${e.message}")
        }
    }

    fun extractEmbedding(frameBitmap: Bitmap, boundingBox: Rect): FloatArray {
        val cropX = max(0, boundingBox.left)
        val cropY = max(0, boundingBox.top)
        val cropWidth = min(frameBitmap.width - cropX, boundingBox.width())
        val cropHeight = min(frameBitmap.height - cropY, boundingBox.height())

        if (cropWidth <= 4 || cropHeight <= 4) {
            return FloatArray(embeddingDim) { 0.0f }
        }

        val croppedFace = try {
            Bitmap.createBitmap(frameBitmap, cropX, cropY, cropWidth, cropHeight)
        } catch (e: Exception) {
            return FloatArray(embeddingDim) { 0.0f }
        }

        val resizedFace = croppedFace.scale(modelInputSize, modelInputSize)

        if (tfliteInterpreter != null) {
            try {
                val inputBuffer = ByteBuffer.allocateDirect(1 * modelInputSize * modelInputSize * 3 * 4)
                inputBuffer.order(ByteOrder.nativeOrder())

                val pixels = IntArray(modelInputSize * modelInputSize)
                resizedFace.getPixels(pixels, 0, modelInputSize, 0, 0, modelInputSize, modelInputSize)

                for (pixel in pixels) {
                    val r = ((Color.red(pixel) - 127.5f) / 128.0f)
                    val g = ((Color.green(pixel) - 127.5f) / 128.0f)
                    val b = ((Color.blue(pixel) - 127.5f) / 128.0f)
                    inputBuffer.putFloat(r)
                    inputBuffer.putFloat(g)
                    inputBuffer.putFloat(b)
                }

                val outputArray = Array(1) { FloatArray(embeddingDim) }
                tfliteInterpreter?.run(inputBuffer, outputArray)
                val rawEmbedding = outputArray[0]
                return normalize(rawEmbedding)
            } catch (e: Exception) {
                Log.w(TAG, "TFLite inference failed, falling back: ${e.message}")
            }
        }

        return extractEnhancedSpatialEmbedding(resizedFace)
    }

    /**
     * Enhanced fallback embedding that uses TWO features per grid cell:
     *   [0..63]  = mean luminance per 8×8 cell  (captures tone / brightness distribution)
     *   [64..127] = luminance VARIANCE per 8×8 cell (captures TEXTURE)
     *
     * Why variance is critical:
     *   - Smooth skin        → near-zero variance
     *   - Hair, beard        → medium variance
     *   - Glasses, shirt collar, high-contrast features → high variance
     *
     * Two people with similar skin tones but different glasses, hair, or clothing will
     * produce very different variance maps, making this embedding far more discriminative
     * than the old mean-only approach which treated all Asian men with similar skin tones
     * as virtually identical.
     */
    private fun extractEnhancedSpatialEmbedding(faceBitmap: Bitmap): FloatArray {
        val width = faceBitmap.width
        val height = faceBitmap.height
        val pixels = IntArray(width * height)
        faceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val embedding = FloatArray(embeddingDim) // 128

        val gridRows = 8
        val gridCols = 8
        val cellW = max(1, width / gridCols)
        val cellH = max(1, height / gridRows)

        val meanLums = FloatArray(64)  // mean luminance per cell
        val varLums  = FloatArray(64)  // luminance variance per cell

        var gridIdx = 0
        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val startY = r * cellH
                val endY   = min(height, (r + 1) * cellH)
                val startX = c * cellW
                val endX   = min(width,  (c + 1) * cellW)

                var sumLum   = 0.0
                var sumLumSq = 0.0
                var count    = 0

                for (y in startY until endY) {
                    for (x in startX until endX) {
                        val px = pixels[y * width + x]
                        val lum = getLuminance(px).toDouble()
                        sumLum   += lum
                        sumLumSq += lum * lum
                        count++
                    }
                }

                if (count > 0) {
                    val meanLum = (sumLum / count).toFloat()
                    // Variance = E[X²] − E[X]²
                    val variance = max(0.0, sumLumSq / count - meanLum * meanLum).toFloat()
                    meanLums[gridIdx] = meanLum
                    varLums[gridIdx]  = variance
                }
                gridIdx++
            }
        }

        // Zero-centre mean luminance features so cosine similarity measures shape, not brightness
        val globalMeanLum = meanLums.average().toFloat()
        for (i in 0 until 64) {
            embedding[i] = meanLums[i] - globalMeanLum
        }

        // Zero-centre variance features; scale by global mean so values are relative, not absolute
        val globalMeanVar = varLums.average().toFloat().coerceAtLeast(1f)
        for (i in 0 until 64) {
            embedding[64 + i] = (varLums[i] - globalMeanVar) / globalMeanVar
        }

        return normalize(embedding)
    }

    private fun normalize(vector: FloatArray): FloatArray {
        var sumSquare = 0.0f
        for (v in vector) {
            sumSquare += v * v
        }
        val norm = sqrt(sumSquare.toDouble()).toFloat()
        if (norm < 1e-6f) return vector

        val result = FloatArray(vector.size)
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }

    private fun getLuminance(color: Int): Float {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (0.299f * r + 0.587f * g + 0.114f * b)
    }

    fun close() {
        try {
            tfliteInterpreter?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        private const val TAG = "FaceEmbeddingEngine"

        fun calculateCosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size || a.isEmpty()) return 0.0f
            var dot   = 0.0f
            var normA = 0.0f
            var normB = 0.0f
            for (i in a.indices) {
                dot   += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denom = sqrt((normA * normB).toDouble()).toFloat()
            if (denom < 1e-6f) return 0.0f
            return dot / denom
        }
    }
}
