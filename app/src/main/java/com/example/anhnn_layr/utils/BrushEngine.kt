package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath

/** Chế độ cọ: xoá vùng, khôi phục từ ảnh gốc, hoặc tô màu lên ảnh. */
enum class BrushMode { ERASE, RESTORE, PAINT }

data class TouchPath(
    val points: List<Offset>,
    val mode: BrushMode,
    val brushSize: Float,
    // Màu ARGB dùng khi mode == PAINT (bỏ qua với ERASE/RESTORE).
    val color: Int = android.graphics.Color.BLACK,
)

fun TouchPath.toComposePath(): Path {
    val p = Path()
    if (points.isEmpty()) return p
    p.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return p
    var last = points[0]
    for (i in 1 until points.size) {
        val cur = points[i]
        val mid = Offset((last.x + cur.x) / 2f, (last.y + cur.y) / 2f)
        p.quadraticTo(last.x, last.y, mid.x, mid.y)
        last = cur
    }
    p.lineTo(last.x, last.y)
    return p
}

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
        val androidPath = tp.toComposePath().asAndroidPath()
        when (tp.mode) {
            BrushMode.ERASE ->
                canvas.drawPath(androidPath, strokePaint(tp.brushSize, PorterDuff.Mode.CLEAR))
            BrushMode.PAINT ->
                canvas.drawPath(androidPath, strokePaint(tp.brushSize, PorterDuff.Mode.SRC_OVER).apply {
                    color = tp.color
                })
            BrushMode.RESTORE -> {
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
    }
    return out
}
