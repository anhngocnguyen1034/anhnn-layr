package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class CropBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

fun centerCropBounds(width: Int, height: Int, aspectWidth: Int, aspectHeight: Int): CropBounds {
    require(width > 0 && height > 0) { "Bitmap size must be positive" }
    require(aspectWidth > 0 && aspectHeight > 0) { "Aspect ratio must be positive" }

    val targetRatio = aspectWidth.toFloat() / aspectHeight.toFloat()
    val currentRatio = width.toFloat() / height.toFloat()
    if (abs(currentRatio - targetRatio) < 0.001f) {
        return CropBounds(left = 0, top = 0, width = width, height = height)
    }

    return if (currentRatio > targetRatio) {
        val cropWidth = (height * targetRatio).roundToInt().coerceIn(1, width)
        val left = (width - cropWidth) / 2
        CropBounds(left = left, top = 0, width = cropWidth, height = height)
    } else {
        val cropHeight = (width / targetRatio).roundToInt().coerceIn(1, height)
        val top = (height - cropHeight) / 2
        CropBounds(left = 0, top = top, width = width, height = cropHeight)
    }
}

fun Bitmap.crop(bounds: CropBounds): Bitmap {
    val cropped = Bitmap.createBitmap(this, bounds.left, bounds.top, bounds.width, bounds.height)
    return cropped.copy(Bitmap.Config.ARGB_8888, true)
}

fun List<TouchPath>.translatedAfterCrop(bounds: CropBounds): List<TouchPath> = map { path ->
    path.copy(
        points = path.points.map { point ->
            Offset(point.x - bounds.left, point.y - bounds.top)
        },
    )
}

/**
 * Khung cắt có thể xoay, định nghĩa theo toạ độ pixel của bitmap (cùng hệ với
 * [TouchPath] và [TextSticker]). [cx]/[cy] là tâm khung, [width]/[height] là kích
 * thước khung, [rotationDeg] là góc xoay (độ, chiều kim đồng hồ).
 */
data class CropFrame(
    val cx: Float,
    val cy: Float,
    val width: Float,
    val height: Float,
    val rotationDeg: Float = 0f,
)

/**
 * Cắt vùng hình chữ nhật (có thể xoay) [frame] khỏi bitmap và "xoay thẳng" lại để
 * ảnh kết quả luôn đứng (axis-aligned). Phần khung lọt ra ngoài ảnh gốc sẽ trong suốt.
 */
fun Bitmap.cropRotated(frame: CropFrame): Bitmap {
    val outW = frame.width.roundToInt().coerceAtLeast(1)
    val outH = frame.height.roundToInt().coerceAtLeast(1)
    val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val matrix = Matrix().apply {
        postTranslate(-frame.cx, -frame.cy)
        postRotate(-frame.rotationDeg)
        postTranslate(outW / 2f, outH / 2f)
    }
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    canvas.drawBitmap(this, matrix, paint)
    return output
}

/** Đưa một điểm trong ảnh gốc về toạ độ ảnh sau khi cắt theo [CropFrame]. */
fun CropFrame.mapPoint(point: Offset): Offset {
    val outW = width.roundToInt().coerceAtLeast(1).toFloat()
    val outH = height.roundToInt().coerceAtLeast(1).toFloat()
    val dx = point.x - cx
    val dy = point.y - cy
    val rad = Math.toRadians(-rotationDeg.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    return Offset(
        x = dx * c - dy * s + outW / 2f,
        y = dx * s + dy * c + outH / 2f,
    )
}

fun List<TouchPath>.transformedByCropFrame(frame: CropFrame): List<TouchPath> = map { path ->
    path.copy(points = path.points.map { frame.mapPoint(it) })
}
