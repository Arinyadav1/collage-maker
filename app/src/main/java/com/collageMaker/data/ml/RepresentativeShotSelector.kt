package com.collageMaker.data.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.collageMaker.data.model.AppearanceSegment
import com.collageMaker.data.model.DetectedFaceFrame
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.scale

class RepresentativeShotSelector {

    fun selectBestShot(
        appearances: List<AppearanceSegment>
    ): DetectedFaceFrame {
        val allFrames = appearances.flatMap { it.faceFrames }
        if (allFrames.isEmpty()) {
            throw IllegalArgumentException("No frames available for shot selection")
        }

        var bestFrame = allFrames.first()
        var highestScore = -100.0f

        for (frame in allFrames) {
            val score = calculateShotQualityScore(frame)
            if (score > highestScore) {
                highestScore = score
                bestFrame = frame
            }
        }

        return bestFrame
    }

    fun calculateShotQualityScore(frame: DetectedFaceFrame): Float {
        val bitmapW = frame.frameBitmap.width
        val bitmapH = frame.frameBitmap.height

        val frontality = max(
            0.0f,
            1.0f - min(1.0f, (abs(frame.eulerY) / 40.0f + abs(frame.eulerZ) / 40.0f))
        )

        val leftEye = frame.leftEyeOpenProb ?: 0.8f
        val rightEye = frame.rightEyeOpenProb ?: 0.8f
        val eyesOpen = (leftEye + rightEye) / 2.0f

        val eyePenalty = if (leftEye < 0.35f || rightEye < 0.35f) 0.45f else 0.0f

        val smile = frame.smilingProb ?: 0.5f

        val sharpness = frame.sharpnessScore

        val box = frame.faceBoundingBox
        val edgeMarginX = bitmapW * 0.04f
        val edgeMarginY = bitmapH * 0.04f
        val isClipped = box.left <= edgeMarginX ||
                box.top <= edgeMarginY ||
                box.right >= (bitmapW - edgeMarginX) ||
                box.bottom >= (bitmapH - edgeMarginY)

        val clippingPenalty = if (isClipped) 0.35f else 0.0f

        val score = (0.35f * frontality) +
                (0.25f * eyesOpen) +
                (0.20f * sharpness) +
                (0.20f * smile) -
                eyePenalty -
                clippingPenalty

        return score
    }

    fun createGenerousFaceTile(
        frameBitmap: Bitmap,
        faceBoundingBox: Rect,
        targetWidth: Int = 600,
        targetHeight: Int = 750
    ): Bitmap {
        val origW = frameBitmap.width
        val origH = frameBitmap.height

        val centerX = faceBoundingBox.exactCenterX()
        val centerY = faceBoundingBox.exactCenterY()

        val faceSize = max(faceBoundingBox.width(), faceBoundingBox.height())
        val generousSize = (faceSize * 2.3f).toInt()

        val desiredAspect = targetWidth.toFloat() / targetHeight.toFloat()

        var cropW = generousSize
        var cropH = (cropW / desiredAspect).toInt()

        if (cropW > origW) {
            cropW = origW
            cropH = (cropW / desiredAspect).toInt()
        }
        if (cropH > origH) {
            cropH = origH
            cropW = (cropH * desiredAspect).toInt()
        }

        var left = (centerX - cropW / 2f).toInt()
        var top = (centerY - cropH / 2.2f).toInt()

        left = max(0, min(origW - cropW, left))
        top = max(0, min(origH - cropH, top))

        val cropped = try {
            Bitmap.createBitmap(frameBitmap, left, top, cropW, cropH)
        } catch (e: Exception) {
            frameBitmap
        }

        return cropped.scale(targetWidth, targetHeight)
    }
}
