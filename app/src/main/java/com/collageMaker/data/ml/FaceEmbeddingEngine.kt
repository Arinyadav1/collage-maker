package com.collageMaker.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
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
        } catch (e: Exception) {
            tfliteInterpreter = null
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
                // Fallback to spatial landmark feature descriptor
            }
        }

        return extractSpatialLandmarkEmbedding(resizedFace)
    }

    private fun extractSpatialLandmarkEmbedding(faceBitmap: Bitmap): FloatArray {
        val width = faceBitmap.width
        val height = faceBitmap.height
        val pixels = IntArray(width * height)
        faceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val embedding = FloatArray(embeddingDim)

        val gridRows = 8
        val gridCols = 8
        val cellW = max(1, width / gridCols)
        val cellH = max(1, height / gridRows)

        val lums = FloatArray(64)
        val rRatios = FloatArray(16)
        val gRatios = FloatArray(16)
        val bRatios = FloatArray(16)

        var gridIdx = 0
        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var count = 0

                val startY = r * cellH
                val endY = min(height, (r + 1) * cellH)
                val startX = c * cellW
                val endX = min(width, (c + 1) * cellW)

                for (y in startY until endY) {
                    for (x in startX until endX) {
                        val px = pixels[y * width + x]
                        sumR += Color.red(px)
                        sumG += Color.green(px)
                        sumB += Color.blue(px)
                        count++
                    }
                }

                if (count > 0) {
                    val avgR = sumR / count
                    val avgG = sumG / count
                    val avgB = sumB / count
                    val totalRgb = max(1.0f, avgR + avgG + avgB)

                    lums[gridIdx] = (0.299f * avgR + 0.587f * avgG + 0.114f * avgB)
                    if (gridIdx < 16) {
                        rRatios[gridIdx] = avgR / totalRgb
                        gRatios[gridIdx] = avgG / totalRgb
                        bRatios[gridIdx] = avgB / totalRgb
                    }
                }
                gridIdx++
            }
        }

        // Subtract mean luminance to zero-center feature values (essential for cosine similarity discriminant)
        val meanLum = lums.average().toFloat()
        var idx = 0

        for (i in 0 until 64) {
            embedding[idx++] = lums[i] - meanLum
        }

        val meanR = rRatios.average().toFloat()
        val meanG = gRatios.average().toFloat()
        val meanB = bRatios.average().toFloat()

        for (i in 0 until 16) {
            if (idx < embeddingDim) embedding[idx++] = (rRatios[i] - meanR) * 100f
            if (idx < embeddingDim) embedding[idx++] = (gRatios[i] - meanG) * 100f
            if (idx < embeddingDim) embedding[idx++] = (bRatios[i] - meanB) * 100f
        }

        while (idx < embeddingDim) {
            val i1 = idx % 64
            val i2 = (idx + 1) % 64
            embedding[idx] = (lums[i1] - lums[i2])
            idx++
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

    companion object {
        fun calculateCosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size || a.isEmpty()) return 0.0f
            var dot = 0.0f
            var normA = 0.0f
            var normB = 0.0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denom = sqrt((normA * normB).toDouble()).toFloat()
            if (denom < 1e-6f) return 0.0f
            return dot / denom
        }
    }
}
