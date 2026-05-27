package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun generateFinalBitmap(
    subjectBitmap: Bitmap,
    bgColor: Color,
    bgBitmap: Bitmap? = null,
    textStickers: List<TextSticker> = emptyList(),
): Bitmap {
    val w = subjectBitmap.width
    val h = subjectBitmap.height
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)

    bgBitmap?.let { src ->
        val srcRect = computeCoverSrcRect(src.width, src.height, w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, srcRect, Rect(0, 0, w, h), paint)
    }

    if (bgColor != Color.Transparent && bgColor.alpha > 0f) {
        canvas.drawColor(bgColor.toArgb())
    }

    canvas.drawBitmap(subjectBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
    drawTextStickers(canvas, textStickers)
    return out
}

private fun computeCoverSrcRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
    val srcRatio = srcW.toFloat() / srcH
    val dstRatio = dstW.toFloat() / dstH
    return if (srcRatio > dstRatio) {
        val cropW = (srcH * dstRatio).toInt()
        val left = (srcW - cropW) / 2
        Rect(left, 0, left + cropW, srcH)
    } else {
        val cropH = (srcW / dstRatio).toInt()
        val top = (srcH - cropH) / 2
        Rect(0, top, srcW, top + cropH)
    }
}
