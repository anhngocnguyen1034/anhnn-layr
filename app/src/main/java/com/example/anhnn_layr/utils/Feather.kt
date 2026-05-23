package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

fun applyFeather(src: Bitmap, radius: Float): Bitmap {
    if (radius <= 0.5f) return src
    val w = src.width
    val h = src.height

    val alphaPaint = Paint().apply {
        isAntiAlias = true
        maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
    }
    val offsetXY = IntArray(2)
    val blurredAlpha = src.extractAlpha(alphaPaint, offsetXY)

    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawBitmap(blurredAlpha, offsetXY[0].toFloat(), offsetXY[1].toFloat(), null)
    val srcInPaint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }
    canvas.drawBitmap(src, 0f, 0f, srcInPaint)

    blurredAlpha.recycle()
    return out
}
