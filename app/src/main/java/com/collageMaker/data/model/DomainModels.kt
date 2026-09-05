package com.collageMaker.data.model

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri

data class VideoItem(
    val id: String,
    val title: String,
    val description: String,
    val uri: Uri? = null,
    val assetFileName: String? = null,
)

data class DetectedFaceFrame(
    val frameTimestampMs: Long,
    val frameBitmap: Bitmap,
    val faceBoundingBox: Rect,
    val embedding: FloatArray,
    val eulerY: Float,
    val eulerZ: Float,
    val leftEyeOpenProb: Float?,
    val rightEyeOpenProb: Float?,
    val smilingProb: Float?,
    val sharpnessScore: Float,
    val qualityScore: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DetectedFaceFrame
        return frameTimestampMs == other.frameTimestampMs && faceBoundingBox == other.faceBoundingBox
    }

    override fun hashCode(): Int {
        var result = frameTimestampMs.hashCode()
        result = 31 * result + faceBoundingBox.hashCode()
        return result
    }
}

data class AppearanceSegment(
    val segmentId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val faceFrames: List<DetectedFaceFrame>,
    val representativeEmbedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AppearanceSegment
        return segmentId == other.segmentId
    }

    override fun hashCode(): Int {
        return segmentId.hashCode()
    }
}

data class PersonClusterResult(
    val personId: Int,
    val appearanceCount: Int,
    val appearances: List<AppearanceSegment>,
    val bestRepresentativeFrame: DetectedFaceFrame,
    val croppedFaceTile: Bitmap
)

enum class CollageStyle {
    SOCIAL_POST,
    STORY_REEL,
    EDITORIAL_CARD
}

data class ProcessingResult(
    val videoTitle: String,
    val personClusters: List<PersonClusterResult>,
    val totalAppearances: Int,
    val generatedCollageUri: Uri?,
    val generatedCollageBitmap: Bitmap?
)

sealed interface ProcessingStatus {
    object Idle : ProcessingStatus
    data class Processing(val stepName: String, val progress: Float) : ProcessingStatus
    data class Success(val result: ProcessingResult) : ProcessingStatus
    data class Error(val message: String) : ProcessingStatus
}

sealed interface SaveStatus {
    object Idle : SaveStatus
    object Saving : SaveStatus
    data class Saved(val filePath: String) : SaveStatus
    data class Error(val message: String) : SaveStatus
}
