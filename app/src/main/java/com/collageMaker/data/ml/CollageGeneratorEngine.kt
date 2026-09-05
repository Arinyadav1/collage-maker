package com.collageMaker.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.collageMaker.data.model.CollageStyle
import com.collageMaker.data.model.PersonClusterResult
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.createBitmap

class CollageGeneratorEngine(private val context: Context) {

    fun generateCollage(
        videoTitle: String,
        personClusters: List<PersonClusterResult>,
        style: CollageStyle = CollageStyle.SOCIAL_POST
    ): Pair<Bitmap, File> {
        val (width, height) = when (style) {
            CollageStyle.SOCIAL_POST -> Pair(1080, 1350) // 4:5 Instagram Portrait Post
            CollageStyle.STORY_REEL -> Pair(1080, 1920) // 9:16 Story / Reel
            CollageStyle.EDITORIAL_CARD -> Pair(1080, 1080) // 1:1 Square Post
        }

        val collageBitmap = createBitmap(width, height)
        val canvas = Canvas(collageBitmap)

        drawBackground(canvas, width, height)

        val totalAppearances = personClusters.sumOf { it.appearanceCount }

        val headerHeight = drawSocialHeader(canvas, width, videoTitle, personClusters.size)

        val footerHeight = drawSocialEngagementFooter(canvas, width, height, personClusters.size, totalAppearances)

        drawPersonTiles(canvas, width, height, headerHeight, footerHeight, personClusters)

        val outputFile = saveBitmapToCache(collageBitmap)
        return Pair(collageBitmap, outputFile)
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.parseColor("#0F172A"),
                Color.parseColor("#18182B"),
                Color.parseColor("#0A0E1A")
            ),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val glow1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E11D48")
            alpha = 20
        }
        canvas.drawCircle(width * 0.85f, height * 0.12f, 320f, glow1)

        val glow2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6366F1")
            alpha = 25
        }
        canvas.drawCircle(width * 0.15f, height * 0.75f, 380f, glow2)
    }

    private fun drawSocialHeader(
        canvas: Canvas,
        width: Int,
        title: String,
        peopleCount: Int
    ): Float {
        val margin = 50f
        val topY = 60f

        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
        }
        canvas.drawCircle(margin + 36f, topY + 36f, 36f, avatarPaint)

        val avatarTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AI", margin + 36f, topY + 46f, avatarTextPaint)

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("@facerecap", margin + 90f, topY + 34f, handlePaint)

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
        }
        canvas.drawCircle(margin + 285f, topY + 24f, 14f, checkPaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val subText = "$title • $peopleCount People Detected"
        canvas.drawText(subText, margin + 90f, topY + 66f, subtitlePaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            strokeWidth = 2f
        }
        val headerBottom = topY + 100f
        canvas.drawLine(margin, headerBottom, width - margin, headerBottom, linePaint)

        return headerBottom + 20f
    }

    private fun drawPersonTiles(
        canvas: Canvas,
        width: Int,
        height: Int,
        headerBottom: Float,
        footerHeight: Float,
        personClusters: List<PersonClusterResult>
    ) {
        if (personClusters.isEmpty()) return

        val margin = 50f
        val availableWidth = width - (margin * 2)
        val availableHeight = height - headerBottom - footerHeight

        val count = personClusters.size
        val cols = when {
            count == 1 -> 1
            count <= 4 -> 2
            else -> 3
        }
        val rows = ceil(count.toDouble() / cols).toInt()

        val gap = 20f
        val tileWidth = (availableWidth - (cols - 1) * gap) / cols
        val tileHeight = min((availableHeight - (rows - 1) * gap) / rows, tileWidth * 1.25f)

        val totalGridHeight = rows * tileHeight + (rows - 1) * gap
        val startY = headerBottom + max(0f, (availableHeight - totalGridHeight) / 2f)

        for ((index, person) in personClusters.withIndex()) {
            val r = index / cols
            val c = index % cols

            val tileLeft = margin + c * (tileWidth + gap)
            val tileTop = startY + r * (tileHeight + gap)
            val tileRight = tileLeft + tileWidth
            val tileBottom = tileTop + tileHeight

            val rectF = RectF(tileLeft, tileTop, tileRight, tileBottom)

            drawSingleSocialTile(canvas, rectF, person)
        }
    }

    private fun drawSingleSocialTile(
        canvas: Canvas,
        rect: RectF,
        person: PersonClusterResult
    ) {
        val cornerRadius = 28f
        val path = Path().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(path)

        val tileBitmap = person.croppedFaceTile
        val srcRect = android.graphics.Rect(0, 0, tileBitmap.width, tileBitmap.height)
        canvas.drawBitmap(tileBitmap, srcRect, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.bottom - rect.height() * 0.45f,
                rect.left, rect.bottom,
                intArrayOf(Color.TRANSPARENT, Color.argb(220, 15, 23, 42)),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(rect.left, rect.bottom - rect.height() * 0.45f, rect.right, rect.bottom, scrimPaint)

        canvas.restore()

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#38BDF8")
            alpha = 160
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        val badgeMargin = 14f
        val badgeH = 42f
        val badgeTop = rect.bottom - badgeH - badgeMargin
        val badgeLeft = rect.left + badgeMargin

        val badgeText = "Person ${person.personId} • ${person.appearanceCount} ${if (person.appearanceCount == 1) "Appearance" else "Appearances"}"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(18f, min(24f, rect.width() * 0.08f))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(badgeText)

        val badgeRect = RectF(badgeLeft, badgeTop, min(rect.right - badgeMargin, badgeLeft + textWidth + 28f), badgeTop + badgeH)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
            alpha = 230
        }
        canvas.drawRoundRect(badgeRect, 21f, 21f, badgeBgPaint)

        canvas.drawText(
            badgeText,
            badgeLeft + 14f,
            badgeTop + badgeH * 0.68f,
            textPaint
        )
    }

    private fun drawSocialEngagementFooter(
        canvas: Canvas,
        width: Int,
        height: Int,
        peopleCount: Int,
        totalAppearances: Int
    ): Float {
        val footerHeight = 160f
        val footerTop = height - footerHeight
        val margin = 50f

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            strokeWidth = 2f
        }
        canvas.drawLine(margin, footerTop, width - margin, footerTop, linePaint)

        val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F43F5E")
        }
        canvas.drawCircle(margin + 20f, footerTop + 36f, 16f, heartPaint)

        val socialTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val engagementText = "Liked by $peopleCount detected people and $totalAppearances appearance segments"
        canvas.drawText(engagementText, margin + 46f, footerTop + 44f, socialTextPaint)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(
            "@facerecap Video summary created on-device with Collage Maker",
            margin,
            footerTop + 86f,
            captionPaint
        )

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(
            "100% On-Device AI • No Cloud Uploads",
            margin,
            footerTop + 124f,
            watermarkPaint
        )

        return footerHeight
    }

    private fun saveBitmapToCache(bitmap: Bitmap): File {
        val cacheDir = context.cacheDir
        val imagesDir = File(cacheDir, "collages")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val file = File(imagesDir, "social_post_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
