package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.Color

data class OutlinePreset(val label: String, val color: Color)

val OUTLINE_PRESETS = listOf(
    OutlinePreset("Trắng", Color(0xFFFFFFFF)),
    OutlinePreset("Đen", Color(0xFF000000)),
    OutlinePreset("Vàng neon", Color(0xFFE5FF00)),
    OutlinePreset("Hồng neon", Color(0xFFFF1493)),
    OutlinePreset("Xanh neon", Color(0xFF00E5FF)),
    OutlinePreset("Cam neon", Color(0xFFFF6F00)),
)

fun applySubjectEffects(
    subject: Bitmap,
    outlineWidth: Float,
    outlineColor: Color,
    shadowRadius: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float,
): Bitmap {
    val noEffects = outlineWidth <= 0.5f &&
        shadowRadius <= 0.5f &&
        brightness == 0f &&
        contrast == 0f &&
        saturation == 0f
    if (noEffects) return subject

    val w = subject.width
    val h = subject.height
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)

    if (shadowRadius > 0.5f) {
        drawShadow(canvas, subject, shadowRadius)
    }
    if (outlineWidth > 0.5f) {
        drawOutline(canvas, subject, outlineWidth, outlineColor)
    }
    drawColoredSubject(canvas, subject, brightness, contrast, saturation)
    return out
}

private fun drawShadow(canvas: Canvas, subject: Bitmap, radius: Float) {
    val alphaPaint = Paint().apply { maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL) }
    val offsetXY = IntArray(2)
    val blurredAlpha = subject.extractAlpha(alphaPaint, offsetXY)
    val tintPaint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0.55f, 0f,
        )))
    }
    canvas.drawBitmap(
        blurredAlpha,
        offsetXY[0].toFloat(),
        offsetXY[1].toFloat() + radius * 0.4f,
        tintPaint,
    )
    blurredAlpha.recycle()
}

private fun drawOutline(canvas: Canvas, subject: Bitmap, width: Float, color: Color) {
    val growPaint = Paint().apply { maskFilter = BlurMaskFilter(width, BlurMaskFilter.Blur.NORMAL) }
    val offsetXY = IntArray(2)
    val grown = subject.extractAlpha(growPaint, offsetXY)
    val r = color.red * 255f
    val g = color.green * 255f
    val b = color.blue * 255f
    val tintPaint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, r,
            0f, 0f, 0f, 0f, g,
            0f, 0f, 0f, 0f, b,
            0f, 0f, 0f, 10f, 0f,
        )))
    }
    canvas.drawBitmap(grown, offsetXY[0].toFloat(), offsetXY[1].toFloat(), tintPaint)
    grown.recycle()
}

private fun drawColoredSubject(
    canvas: Canvas,
    subject: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    if (brightness != 0f || contrast != 0f || saturation != 0f) {
        paint.colorFilter = ColorMatrixColorFilter(
            buildAdjustmentMatrix(brightness, contrast, saturation)
        )
    }
    canvas.drawBitmap(subject, 0f, 0f, paint)
}

private fun buildAdjustmentMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
    val satMatrix = ColorMatrix().apply { setSaturation((saturation + 1f).coerceIn(0f, 2f)) }
    val c = (contrast + 1f).coerceIn(0f, 2f)
    val cOff = (1f - c) * 127.5f
    val contrastMatrix = ColorMatrix(floatArrayOf(
        c, 0f, 0f, 0f, cOff,
        0f, c, 0f, 0f, cOff,
        0f, 0f, c, 0f, cOff,
        0f, 0f, 0f, 1f, 0f,
    ))
    val brightOff = brightness * 255f
    val brightMatrix = ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, brightOff,
        0f, 1f, 0f, 0f, brightOff,
        0f, 0f, 1f, 0f, brightOff,
        0f, 0f, 0f, 1f, 0f,
    ))
    return ColorMatrix().apply {
        postConcat(satMatrix)
        postConcat(contrastMatrix)
        postConcat(brightMatrix)
    }
}
