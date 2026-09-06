package com.collageMaker.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.collageMaker.data.model.CollageStyle
import com.collageMaker.data.model.PersonClusterResult
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class CollageGeneratorEngine(private val context: Context) {

    fun generateCollage(
        videoTitle: String,
        personClusters: List<PersonClusterResult>,
        style: CollageStyle = CollageStyle.SOCIAL_POST
    ): Pair<Bitmap, File> {
        val width = 1080
        val height = 1080 // Square dynamic photo collage canvas

        val collageBitmap = createBitmap(width, height)
        val canvas = Canvas(collageBitmap)

        // Clean white background between dynamic tiles
        canvas.drawColor(Color.WHITE)

        val tiles = calculateDynamicMosaicRects(width.toFloat(), height.toFloat(), personClusters.size)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        for ((index, rect) in tiles.withIndex()) {
            if (index >= personClusters.size) break
            val person = personClusters[index]
            val bitmap = person.croppedFaceTile ?: person.bestRepresentativeFrame.frameBitmap

            drawCenterCropTile(canvas, bitmap, rect, paint)
        }

        val outputFile = saveBitmapToCache(collageBitmap)
        return Pair(collageBitmap, outputFile)
    }

    private fun calculateDynamicMosaicRects(
        totalW: Float,
        totalH: Float,
        count: Int
    ): List<RectF> {
        if (count <= 0) return emptyList()

        val padding = 8f
        val gap = 8f
        val rects = mutableListOf<RectF>()

        val availableW = totalW - (padding * 2)
        val availableH = totalH - (padding * 2)
        val startX = padding
        val startY = padding

        when (count) {
            1 -> {
                rects.add(RectF(startX, startY, startX + availableW, startY + availableH))
            }
            2 -> {
                // Split 50/50 vertically
                val tileW = (availableW - gap) / 2f
                rects.add(RectF(startX, startY, startX + tileW, startY + availableH))
                rects.add(RectF(startX + tileW + gap, startY, startX + availableW, startY + availableH))
            }
            3 -> {
                // 1 large hero tile left, 2 stacked right
                val leftW = (availableW - gap) * 0.52f
                val rightW = availableW - leftW - gap
                val rightH = (availableH - gap) / 2f

                rects.add(RectF(startX, startY, startX + leftW, startY + availableH))
                rects.add(RectF(startX + leftW + gap, startY, startX + availableW, startY + rightH))
                rects.add(RectF(startX + leftW + gap, startY + rightH + gap, startX + availableW, startY + availableH))
            }
            4 -> {
                // 2x2 symmetrical grid
                val tileW = (availableW - gap) / 2f
                val tileH = (availableH - gap) / 2f

                rects.add(RectF(startX, startY, startX + tileW, startY + tileH))
                rects.add(RectF(startX + tileW + gap, startY, startX + availableW, startY + tileH))
                rects.add(RectF(startX, startY + tileH + gap, startX + tileW, startY + availableH))
                rects.add(RectF(startX + tileW + gap, startY + tileH + gap, startX + availableW, startY + availableH))
            }
            5 -> {
                // Dynamic asymmetric layout matching User's reference (Image 2)
                // Left column: 2 tall tiles
                // Right column: 3 stacked tiles
                val leftW = (availableW - gap) / 2f
                val rightW = availableW - leftW - gap

                val leftH = (availableH - gap) / 2f
                val rightH = (availableH - (gap * 2)) / 3f

                // Left top & bottom
                rects.add(RectF(startX, startY, startX + leftW, startY + leftH))
                rects.add(RectF(startX, startY + leftH + gap, startX + leftW, startY + availableH))

                // Right top, middle & bottom
                rects.add(RectF(startX + leftW + gap, startY, startX + availableW, startY + rightH))
                rects.add(RectF(startX + leftW + gap, startY + rightH + gap, startX + availableW, startY + (rightH * 2) + gap))
                rects.add(RectF(startX + leftW + gap, startY + (rightH * 2) + (gap * 2), startX + availableW, startY + availableH))
            }
            6 -> {
                // 2 columns, 3 rows each
                val colW = (availableW - gap) / 2f
                val rowH = (availableH - (gap * 2)) / 3f

                for (r in 0 until 3) {
                    for (c in 0 until 2) {
                        val left = startX + c * (colW + gap)
                        val top = startY + r * (rowH + gap)
                        rects.add(RectF(left, top, left + colW, top + rowH))
                    }
                }
            }
            else -> {
                // 3 columns grid for 7+ items
                val cols = 3
                val rows = (count + cols - 1) / cols
                val tileW = (availableW - (gap * (cols - 1))) / cols
                val tileH = (availableH - (gap * (rows - 1))) / rows

                for (i in 0 until count) {
                    val r = i / cols
                    val c = i % cols
                    val left = startX + c * (tileW + gap)
                    val top = startY + r * (tileH + gap)
                    rects.add(RectF(left, top, left + tileW, top + tileH))
                }
            }
        }

        return rects
    }

    private fun drawCenterCropTile(canvas: Canvas, bitmap: Bitmap, destRect: RectF, paint: Paint) {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        val destWidth = destRect.width()
        val destHeight = destRect.height()

        if (srcWidth <= 0f || srcHeight <= 0f || destWidth <= 0f || destHeight <= 0f) return

        val srcAspect = srcWidth / srcHeight
        val destAspect = destWidth / destHeight

        val srcCropRect: Rect
        if (srcAspect > destAspect) {
            val targetSrcW = srcHeight * destAspect
            val left = ((srcWidth - targetSrcW) / 2f).toInt()
            srcCropRect = Rect(left, 0, min(bitmap.width, (left + targetSrcW).toInt()), bitmap.height)
        } else {
            val targetSrcH = srcWidth / destAspect
            val top = ((srcHeight - targetSrcH) / 2f).toInt()
            srcCropRect = Rect(0, top, bitmap.width, min(bitmap.height, (top + targetSrcH).toInt()))
        }

        canvas.drawBitmap(bitmap, srcCropRect, destRect, paint)
    }

    private fun saveBitmapToCache(bitmap: Bitmap): File {
        val cacheDir = context.cacheDir
        val imagesDir = File(cacheDir, "collages")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val file = File(imagesDir, "dynamic_collage_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
