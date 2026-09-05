package com.collageMaker.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale

class VideoFrameExtractor(private val context: Context) {

    data class ExtractedFrame(
        val timestampMs: Long,
        val bitmap: Bitmap
    )

    suspend fun extractFramesFromUri(
        uri: Uri,
        sampleIntervalMs: Long = 250L,
        onProgress: (Float) -> Unit = {}
    ): List<ExtractedFrame> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 10000L

            val frameList = mutableListOf<ExtractedFrame>()
            var currentMs = 0L

            while (currentMs < durationMs) {
                val timeUs = currentMs * 1000L
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)

                if (bitmap != null) {
                    val scaled = scaleBitmapIfNeeded(bitmap, maxDimension = 720)
                    frameList.add(ExtractedFrame(currentMs, scaled))
                }

                currentMs += sampleIntervalMs
                onProgress(minOf(1.0f, currentMs.toFloat() / durationMs.toFloat()))
            }

            frameList
        } catch (e: Exception) {
            emptyList()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release error
            }
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap

        val aspectRatio = w.toFloat() / h.toFloat()
        val newW: Int
        val newH: Int
        if (w > h) {
            newW = maxDimension
            newH = (maxDimension / aspectRatio).toInt()
        } else {
            newH = maxDimension
            newW = (maxDimension * aspectRatio).toInt()
        }

        return bitmap.scale(newW, newH)
    }
}
