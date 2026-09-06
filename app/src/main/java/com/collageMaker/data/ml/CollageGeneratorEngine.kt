package com.collageMaker.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
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
        val (width, height) = when (style) {
            CollageStyle.STORY_REEL -> 1080 to 1920
            CollageStyle.SOCIAL_POST -> 1080 to 1350
            CollageStyle.EDITORIAL_CARD -> 1080 to 1080
        }

        val collageBitmap = createBitmap(width, height)
        val canvas = Canvas(collageBitmap)

        canvas.drawColor(Color.rgb(15, 18, 30))

        // A collage is an identity recap: each clustered person contributes exactly one tile.
        val faceTiles = personClusters.map { it.croppedFaceTile }
        val headerHeight = if (style == CollageStyle.EDITORIAL_CARD) 118f else 150f
        val tiles = calculateDynamicMosaicRects(
            width.toFloat(),
            height.toFloat() - headerHeight,
            faceTiles.size,
            topInset = headerHeight
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        for ((index, rect) in tiles.withIndex()) {
            if (index >= faceTiles.size) break
            val bitmap = faceTiles[index]
            drawCenterCropTile(canvas, bitmap, rect, paint)
            drawPersonBadge(canvas, rect, personClusters[index], paint)
        }
        drawHeader(canvas, videoTitle, personClusters.size, headerHeight, paint)

        val outputFile = saveBitmapToCache(collageBitmap)
        return Pair(collageBitmap, outputFile)
    }

    private fun calculateDynamicMosaicRects(
        totalW: Float,
        totalH: Float,
        count: Int,
        topInset: Float = 0f
    ): List<RectF> {
        if (count <= 0) return emptyList()

        val padding = 8f
        val gap = 8f
        val rects = mutableListOf<RectF>()

        val availableW = totalW - (padding * 2)
        val availableH = totalH - (padding * 2)
        val startX = padding
        val startY = padding + topInset

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

    private fun drawHeader(
        canvas: Canvas,
        videoTitle: String,
        peopleCount: Int,
        headerHeight: Float,
        paint: Paint
    ) {
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 43f
        paint.color = Color.WHITE
        canvas.drawText("PEOPLE RECAP", 34f, 57f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 25f
        paint.color = Color.rgb(190, 199, 225)
        val subtitle = "$peopleCount ${if (peopleCount == 1) "person" else "people"} • $videoTitle"
        canvas.drawText(subtitle.take(58), 34f, 98f, paint)
        paint.color = Color.rgb(115, 91, 255)
        canvas.drawRect(34f, headerHeight - 18f, 174f, headerHeight - 10f, paint)
    }

    private fun drawPersonBadge(
        canvas: Canvas,
        rect: RectF,
        person: PersonClusterResult,
        paint: Paint
    ) {
        val badge = RectF(rect.left + 14f, rect.bottom - 65f, rect.left + 215f, rect.bottom - 14f)
        paint.color = Color.argb(205, 10, 12, 22)
        canvas.drawRoundRect(badge, 25f, 25f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 22f
        paint.color = Color.WHITE
        canvas.drawText("PERSON ${person.personId}", badge.left + 16f, badge.top + 23f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 18f
        paint.color = Color.rgb(214, 210, 255)
        val appearances = "${person.appearanceCount} ${if (person.appearanceCount == 1) "appearance" else "appearances"}"
        canvas.drawText(appearances, badge.left + 16f, badge.top + 43f, paint)
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
