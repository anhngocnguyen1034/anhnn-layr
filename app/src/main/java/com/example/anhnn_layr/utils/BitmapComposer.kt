package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun generateFinalBitmap(subjectBitmap: Bitmap, bgColor: Color): Bitmap {
    val out = Bitmap.createBitmap(
        subjectBitmap.width,
        subjectBitmap.height,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(out)
    if (bgColor != Color.Transparent && bgColor.alpha > 0f) {
        canvas.drawColor(bgColor.toArgb())
    }
    canvas.drawBitmap(subjectBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
    return out
}
