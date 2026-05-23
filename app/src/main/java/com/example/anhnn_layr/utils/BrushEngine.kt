package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.asAndroidPath

data class TouchPath(
    val path: androidx.compose.ui.graphics.Path,
    val isErase: Boolean,
    val brushSize: Float,
)

private fun strokePaint(brushSize: Float, mode: PorterDuff.Mode): Paint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
    strokeWidth = brushSize
    xfermode = PorterDuffXfermode(mode)
}

fun buildWorkingBitmap(
    processed: Bitmap,
    original: Bitmap,
    paths: List<TouchPath>,
): Bitmap {
    val out = processed.copy(Bitmap.Config.ARGB_8888, true)
    if (paths.isEmpty()) return out
    val canvas = Canvas(out)
    for (tp in paths) {
        val androidPath: Path = tp.path.asAndroidPath()
        if (tp.isErase) {
            canvas.drawPath(androidPath, strokePaint(tp.brushSize, PorterDuff.Mode.CLEAR))
        } else {
            val saved = canvas.saveLayer(null, null)
            canvas.drawPath(androidPath, strokePaint(tp.brushSize, PorterDuff.Mode.SRC_OVER).apply {
                color = android.graphics.Color.BLACK
            })
            val bmpPaint = Paint().apply {
                isAntiAlias = true
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(original, 0f, 0f, bmpPaint)
            canvas.restoreToCount(saved)
        }
    }
    return out
}
