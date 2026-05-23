package com.example.anhnn_layr.utils

import android.graphics.Bitmap

fun blurBackground(src: Bitmap, intensity: Float): Bitmap {
    if (intensity <= 0.5f) return src
    val clamped = intensity.coerceIn(0f, 25f)
    val scale = (1f / (1f + clamped * 0.5f)).coerceIn(0.04f, 1f)
    val smallW = (src.width * scale).toInt().coerceAtLeast(2)
    val smallH = (src.height * scale).toInt().coerceAtLeast(2)
    val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
    val blurred = Bitmap.createScaledBitmap(small, src.width, src.height, true)
    if (small !== blurred) small.recycle()
    return blurred
}
