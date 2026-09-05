package com.collageMaker.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.collageMaker.data.ml.AppearanceClusteringEngine
import com.collageMaker.data.ml.CollageGeneratorEngine
import com.collageMaker.data.ml.FaceDetectorEngine
import com.collageMaker.data.ml.FaceEmbeddingEngine
import com.collageMaker.data.ml.RepresentativeShotSelector
import com.collageMaker.data.model.CollageStyle
import com.collageMaker.data.model.DetectedFaceFrame
import com.collageMaker.data.model.PersonClusterResult
import com.collageMaker.data.model.ProcessingResult
import com.collageMaker.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.toColorInt

class VideoRepository(private val context: Context) {

    private val frameExtractor = VideoFrameExtractor(context)
    private val faceDetectorEngine = FaceDetectorEngine()
    private val faceEmbeddingEngine = FaceEmbeddingEngine(context)
    private val clusteringEngine = AppearanceClusteringEngine()
    private val shotSelector = RepresentativeShotSelector()
    private val collageGenerator = CollageGeneratorEngine(context)

    suspend fun processVideo(
        videoItem: VideoItem,
        collageStyle: CollageStyle = CollageStyle.SOCIAL_POST,
        onProgress: (stepName: String, progress: Float) -> Unit
    ): ProcessingResult = withContext(Dispatchers.Default) {
        val detectedFaceFrames = mutableListOf<DetectedFaceFrame>()

        if (videoItem.uri != null) {
            onProgress("Extracting Video Frames...", 0.05f)
            val frames = frameExtractor.extractFramesFromUri(videoItem.uri) { p ->
                onProgress("Extracting Video Frames...", 0.05f + (p * 0.25f))
            }

            onProgress("Detecting Faces (ML Kit)...", 0.30f)
            for ((idx, frame) in frames.withIndex()) {
                val rawFaces = faceDetectorEngine.detectFaces(frame.bitmap)
                for (rawFace in rawFaces) {
                    val embedding = faceEmbeddingEngine.extractEmbedding(frame.bitmap, rawFace.boundingBox)
                    val frameObj = DetectedFaceFrame(
                        frameTimestampMs = frame.timestampMs,
                        frameBitmap = frame.bitmap,
                        faceBoundingBox = rawFace.boundingBox,
                        embedding = embedding,
                        eulerY = rawFace.eulerY,
                        eulerZ = rawFace.eulerZ,
                        leftEyeOpenProb = rawFace.leftEyeOpenProb,
                        rightEyeOpenProb = rawFace.rightEyeOpenProb,
                        smilingProb = rawFace.smilingProb,
                        sharpnessScore = rawFace.sharpnessScore,
                        qualityScore = 0f
                    )
                    detectedFaceFrames.add(frameObj)
                }
                val p = (idx + 1).toFloat() / frames.size
                onProgress("Detecting Faces (ML Kit)...", 0.30f + (p * 0.25f))
            }
        }

        onProgress("Clustering Appearances Across Segments...", 0.60f)
        val personClusterPairs = clusteringEngine.clusterVideoFaceFrames(detectedFaceFrames)

        onProgress("Selecting Strong Representative Shots...", 0.80f)
        val personClusterResults = mutableListOf<PersonClusterResult>()

        for ((personId, appearances) in personClusterPairs) {
            val bestFrame = shotSelector.selectBestShot(appearances)
            val generousFaceTile = shotSelector.createGenerousFaceTile(
                frameBitmap = bestFrame.frameBitmap,
                faceBoundingBox = bestFrame.faceBoundingBox
            )

            personClusterResults.add(
                PersonClusterResult(
                    personId = personId,
                    appearanceCount = appearances.size,
                    appearances = appearances,
                    bestRepresentativeFrame = bestFrame,
                    croppedFaceTile = generousFaceTile
                )
            )
        }

        onProgress("Generating Story Collage...", 0.90f)
        val (collageBitmap, collageFile) = collageGenerator.generateCollage(
            videoTitle = videoItem.title,
            personClusters = personClusterResults,
            style = collageStyle
        )

        val collageUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                collageFile
            )
        } catch (e: Exception) {
            null
        }

        onProgress("Complete!", 1.0f)

        ProcessingResult(
            videoTitle = videoItem.title,
            personClusters = personClusterResults,
            totalAppearances = personClusterResults.sumOf { it.appearanceCount },
            generatedCollageUri = collageUri,
            generatedCollageBitmap = collageBitmap
        )
    }

    suspend fun saveCollageToGallery(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val filename = "Collage_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CollageMaker")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        resolver.openOutputStream(uri).use { out ->
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        uri.toString()
    }

    private fun generateSyntheticSampleDataset(sampleId: String): List<DetectedFaceFrame> {
        val faceFrames = mutableListOf<DetectedFaceFrame>()
        val width = 720
        val height = 1280

        val colors = listOf(
            "#EF4444".toColorInt(),
            "#3B82F6".toColorInt(),
            "#10B981".toColorInt(),
            "#F59E0B".toColorInt(),
            "#8B5CF6".toColorInt(),
            "#EC4899".toColorInt()
        )

        val peopleCount = when (sampleId) {
            "sample_1" -> 5
            "sample_2" -> 3
            else -> 4
        }
        val appearancesPerPerson = when (sampleId) {
            "sample_1" -> 4
            "sample_2" -> 3
            else -> 2
        }

        var globalTime = 0L

        for (personIndex in 0 until peopleCount) {
            val baseColor = colors[personIndex % colors.size]

            for (appIdx in 0 until appearancesPerPerson) {
                val segStartTime = globalTime + (appIdx * 6000L) + (personIndex * 800L)
                val duration = 2500L

                val isDuoOverlap = (sampleId == "sample_1" && appIdx == 1 && (personIndex == 0 || personIndex == 1))
                val isDuoOverlapCD = (sampleId == "sample_1" && appIdx == 2 && (personIndex == 2 || personIndex == 3))

                val actualSegStart = when {
                    isDuoOverlap -> 10100L
                    isDuoOverlapCD -> 20200L
                    else -> segStartTime
                }

                for (offset in 0L..duration step 300L) {
                    val frameTime = actualSegStart + offset

                    val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(frameBitmap)
                    canvas.drawColor(Color.parseColor("#0B0F19"))

                    val faceX = when {
                        isDuoOverlap && personIndex == 0 -> 200
                        isDuoOverlap && personIndex == 1 -> 520
                        isDuoOverlapCD && personIndex == 2 -> 200
                        isDuoOverlapCD && personIndex == 3 -> 520
                        else -> 360
                    }
                    val faceY = 480

                    val faceRect = Rect(faceX - 110, faceY - 140, faceX + 110, faceY + 140)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = baseColor
                    }
                    canvas.drawCircle(faceX.toFloat(), faceY.toFloat(), 120f, paint)

                    val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                    canvas.drawCircle(faceX - 40f, faceY - 30f, 20f, eyePaint)
                    canvas.drawCircle(faceX + 40f, faceY - 30f, 20f, eyePaint)

                    val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
                    canvas.drawCircle(faceX - 40f, faceY - 30f, 8f, pupilPaint)
                    canvas.drawCircle(faceX + 40f, faceY - 30f, 8f, pupilPaint)

                    val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 8f
                    }
                    canvas.drawArc(
                        faceX - 40f, faceY + 10f, faceX + 40f, faceY + 50f,
                        0f, 180f, false, mouthPaint
                    )

                    val embedding = FloatArray(128) { i ->
                        (personIndex * 1.5f + (i % 7) * 0.1f)
                    }

                    faceFrames.add(
                        DetectedFaceFrame(
                            frameTimestampMs = frameTime,
                            frameBitmap = frameBitmap,
                            faceBoundingBox = faceRect,
                            embedding = embedding,
                            eulerY = (offset % 500 - 250) / 50f,
                            eulerZ = 0f,
                            leftEyeOpenProb = 0.95f,
                            rightEyeOpenProb = 0.95f,
                            smilingProb = 0.88f,
                            sharpnessScore = 0.85f,
                            qualityScore = 0.90f
                        )
                    )
                }
            }
        }

        return faceFrames
    }
}
