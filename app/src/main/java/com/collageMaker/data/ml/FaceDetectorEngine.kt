package com.collageMaker.data.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
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
        .setMinFaceSize(0.12f)
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

        return faces.map { face ->
            val bounds = face.boundingBox
            val clampedBounds = Rect(
                max(0, bounds.left),
                max(0, bounds.top),
                min(bitmap.width, bounds.right),
                min(bitmap.height, bounds.bottom)
            )

            val sharpness = calculateSharpness(bitmap, clampedBounds)

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

    private fun calculateSharpness(bitmap: Bitmap, rect: Rect): Float {
        if (rect.width() <= 5 || rect.height() <= 5) return 0.5f

        val startX = rect.left
        val startY = rect.top
        val width = rect.width()
        val height = rect.height()

        val pixels = IntArray(width * height)
        try {
            bitmap.getPixels(pixels, 0, width, startX, startY, width, height)
        } catch (e: Exception) {
            return 0.5f
        }

        var totalGradient = 0.0
        var count = 0

        val step = max(1, width / 40)
        for (y in 1 until height - 1 step step) {
            for (x in 1 until width - 1 step step) {
                val idx = y * width + x
                val center = getLuminance(pixels[idx])
                val right = getLuminance(pixels[idx + 1])
                val bottom = getLuminance(pixels[idx + width])

                val dx = abs(center - right)
                val dy = abs(center - bottom)
                totalGradient += (dx + dy)
                count++
            }
        }

        if (count == 0) return 0.5f
        val avgGradient = (totalGradient / count).toFloat()
        return min(1.0f, avgGradient / 50.0f)
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
}
