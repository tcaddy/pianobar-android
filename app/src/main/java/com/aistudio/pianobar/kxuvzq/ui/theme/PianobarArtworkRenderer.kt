package com.aistudio.pianobar.kxuvzq.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.aistudio.pianobar.kxuvzq.R

/**
 * Renders theme-matched placeholder artwork and themed borders for surfaces outside
 * Compose (the Android home-screen widget and the MediaStyle lockscreen/notification).
 */
object PianobarArtworkRenderer {

    fun renderThemedArtwork(
        context: Context,
        rawArtwork: Bitmap?,
        palette: PianobarPalette,
        targetPx: Int = 160,
        cornerRadiusRatio: Float = 0.16f
    ): Bitmap {
        return if (rawArtwork != null && !rawArtwork.isRecycled) {
            frameArtworkForTheme(rawArtwork, palette, targetPx, cornerRadiusRatio)
        } else {
            renderThemedPlaceholder(context, palette, targetPx, cornerRadiusRatio)
        }
    }

    fun frameArtworkForTheme(
        source: Bitmap,
        palette: PianobarPalette,
        targetPx: Int = 160,
        cornerRadiusRatio: Float = 0.16f
    ): Bitmap {
        val safeSize = targetPx.coerceAtLeast(32)
        val minSide = minOf(source.width, source.height).coerceAtLeast(1)
        val cropX = (source.width - minSide) / 2
        val cropY = (source.height - minSide) / 2
        val square = if (source.width == source.height) {
            source
        } else {
            Bitmap.createBitmap(source, cropX, cropY, minSide, minSide)
        }
        val scaled = if (square.width == safeSize && square.height == safeSize) {
            square
        } else {
            Bitmap.createScaledBitmap(square, safeSize, safeSize, true)
        }

        val output = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bounds = RectF(0f, 0f, safeSize.toFloat(), safeSize.toFloat())
        val radius = safeSize * cornerRadiusRatio

        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(bounds, radius, radius, artPaint)

        // Crisp theme rim around album artwork so lockscreen & widget carry the active palette
        val strokeWidth = (safeSize * 0.032f).coerceAtLeast(3f)
        val inset = strokeWidth / 2f
        val strokeRect = RectF(inset, inset, safeSize - inset, safeSize - inset)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            if (palette.id == AiSlopPalette.id) {
                shader = LinearGradient(
                    0f, 0f, safeSize.toFloat(), safeSize.toFloat(),
                    intArrayOf(0xFFC084FC.toInt(), 0xFF818CF8.toInt(), 0xFF22D3EE.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
            } else {
                color = palette.accent.toArgb()
            }
        }
        canvas.drawRoundRect(strokeRect, (radius - inset).coerceAtLeast(2f), (radius - inset).coerceAtLeast(2f), strokePaint)

        if (scaled !== square && scaled !== source) scaled.recycle()
        if (square !== source) square.recycle()
        return output
    }

    fun renderThemedPlaceholder(
        context: Context,
        palette: PianobarPalette,
        targetPx: Int = 160,
        cornerRadiusRatio: Float = 0.16f
    ): Bitmap {
        val safeSize = targetPx.coerceAtLeast(32)
        val s = safeSize.toFloat()
        val radius = s * cornerRadiusRatio
        val output = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val clipPath = Path().apply {
            addRoundRect(RectF(0f, 0f, s, s), radius, radius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        // Base surface fill
        canvas.drawColor(palette.surface.toArgb())

        when (palette.id) {
            AiSlopPalette.id -> {
                // Subtle cyan grid + glowing purple & cyan orbs
                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x1A22D3EE
                    strokeWidth = (s * 0.008f).coerceAtLeast(1f)
                }
                val step = s / 6f
                var pos = step
                while (pos < s) {
                    canvas.drawLine(pos, 0f, pos, s, gridPaint)
                    canvas.drawLine(0f, pos, s, pos, gridPaint)
                    pos += step
                }
                val purpleOrb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        s * 0.2f, s * 0.2f, s * 0.7f,
                        intArrayOf(0x66C084FC, 0x00C084FC),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, s, s, purpleOrb)
                val cyanOrb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        s * 0.85f, s * 0.85f, s * 0.7f,
                        intArrayOf(0x5522D3EE, 0x0022D3EE),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, s, s, cyanOrb)
            }
            AmberCrtPalette.id -> {
                // Faint horizontal phosphor CRT scanlines
                val scanPaint = Paint().apply {
                    color = 0x18FFB000
                    strokeWidth = (s * 0.008f).coerceAtLeast(1f)
                }
                val step = (s / 16f).coerceAtLeast(4f)
                var y = step
                while (y < s) {
                    canvas.drawLine(0f, y, s, y, scanPaint)
                    y += step
                }
            }
            else -> {
                // Soft tonal badge vignette
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        s * 0.5f, s * 0.5f, s * 0.65f,
                        intArrayOf(
                            palette.accent.copy(alpha = if (palette.isDark) 0.16f else 0.10f).toArgb(),
                            palette.background.copy(alpha = 0.0f).toArgb()
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, s, s, glow)
            }
        }

        // Inner medallion backing
        val medalRadius = s * 0.28f
        val medalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.surfaceElevated.toArgb()
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, medalRadius, medalPaint)

        // Centered tinted music note glyph
        ContextCompat.getDrawable(context, R.drawable.ic_notif_music)?.mutate()?.let { drawable ->
            val iconHalf = (s * 0.19f).toInt()
            val cx = safeSize / 2
            val cy = safeSize / 2
            drawable.setBounds(cx - iconHalf, cy - iconHalf, cx + iconHalf, cy + iconHalf)
            drawable.setColorFilter(palette.accent.toArgb(), PorterDuff.Mode.SRC_IN)
            drawable.draw(canvas)
        }

        canvas.restore()

        // Crisp theme border stroke
        val strokeWidth = (s * 0.025f).coerceAtLeast(2.5f)
        val inset = strokeWidth / 2f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            if (palette.id == AiSlopPalette.id) {
                shader = LinearGradient(
                    0f, 0f, s, s,
                    intArrayOf(0xFFC084FC.toInt(), 0xFF818CF8.toInt(), 0xFF22D3EE.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
            } else {
                color = palette.accent.copy(alpha = 0.7f).toArgb()
            }
        }
        canvas.drawRoundRect(
            RectF(inset, inset, s - inset, s - inset),
            (radius - inset).coerceAtLeast(2f),
            (radius - inset).coerceAtLeast(2f),
            strokePaint
        )

        return output
    }
}
