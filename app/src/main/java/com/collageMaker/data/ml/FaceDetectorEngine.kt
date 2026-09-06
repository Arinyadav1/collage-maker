package com.collageMaker.data.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FaceDetectorEngine {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.08f)
        .build()

    private val faceDetector = FaceDetection.getClient(detectorOptions)

    data class RawFaceDetection(
        val boundingBox: Rect,
        val eulerY: Float,
        val eulerZ: Float,
        val leftEyeOpenProb: Float?,
        val rightEyeOpenProb: Float?,
        val smilingProb: Float?,
        val sharpnessScore: Float
    )

    fun detectFaces(bitmap: Bitmap): List<RawFaceDetection> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val task = faceDetector.process(inputImage)
        val faces: List<Face> = try {
            Tasks.await(task)
        } catch (e: Exception) {
            emptyList()
        }

        val result = faces.mapNotNull { face ->
            val bounds = face.boundingBox
            val clampedBounds = Rect(
                max(0, bounds.left),
                max(0, bounds.top),
                min(bitmap.width, bounds.right),
                min(bitmap.height, bounds.bottom)
            )

            if (clampedBounds.width() <= 20 || clampedBounds.height() <= 20) {
                return@mapNotNull null
            }

            val sharpness = calculateSharpness(bitmap, clampedBounds)

            // Filter out blurry out-of-focus background blobs and non-human artifacts.
            // Threshold lowered to 0.10 so that valid side-facing / background faces
            // are not silently dropped before tracking begins.
            if (sharpness < 0.10f) {
                null
            } else {
                RawFaceDetection(
                    boundingBox = clampedBounds,
                    eulerY = face.headEulerAngleY,
                    eulerZ = face.headEulerAngleZ,
                    leftEyeOpenProb = face.leftEyeOpenProbability,
                    rightEyeOpenProb = face.rightEyeOpenProbability,
                    smilingProb = face.smilingProbability,
                    sharpnessScore = sharpness
                )
            }
        }
        if (faces.isNotEmpty()) {
            Log.d(TAG, "Frame ${bitmap.width}×${bitmap.height}: MLKit=${faces.size} faces, kept=${result.size} after quality filter")
        }
        return result
    }

    private fun calculateSharpness(bitmap: Bitmap, rect: Rect): Float {
        val startX = max(0, rect.left)
        val startY = max(0, rect.top)
        val width = min(bitmap.width - startX, rect.width())
        val height = min(bitmap.height - startY, rect.height())

        if (width <= 16 || height <= 16) return 0.0f

        val pixels = IntArray(width * height)
        try {
            bitmap.getPixels(pixels, 0, width, startX, startY, width, height)
        } catch (e: Exception) {
            return 0.0f
        }

        var sumLaplacian = 0.0
        var sumLaplacianSq = 0.0
        var count = 0

        val step = max(1, min(width, height) / 50)
        for (y in 1 until height - 1 step step) {
            for (x in 1 until width - 1 step step) {
                val idx = y * width + x
                val center = getLuminance(pixels[idx])
                val left = getLuminance(pixels[idx - 1])
                val right = getLuminance(pixels[idx + 1])
                val top = getLuminance(pixels[idx - width])
                val bottom = getLuminance(pixels[idx + width])

                val lap = (4 * center - left - right - top - bottom).toDouble()
                sumLaplacian += lap
                sumLaplacianSq += lap * lap
                count++
            }
        }

        if (count == 0) return 0.0f

        val mean = sumLaplacian / count
        val variance = (sumLaplacianSq / count) - (mean * mean)

        // Blurry out-of-focus background blobs have variance < 150.0
        // Clear in-focus human faces have variance > 400.0 (up to 3000.0)
        val score = (variance / 800.0).toFloat()
        return min(1.0f, max(0.0f, score))
    }

    private fun getLuminance(color: Int): Float {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (0.299f * r + 0.587f * g + 0.114f * b)
    }

    fun close() {
        try {
            faceDetector.close()
        } catch (e: Exception) {
            // Ignore close error
        }
    }

    companion object {
        private const val TAG = "FaceDetector"
    }
}
