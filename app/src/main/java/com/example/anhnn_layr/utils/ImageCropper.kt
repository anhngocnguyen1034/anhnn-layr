package com.example.anhnn_layr.utils

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.roundToInt

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
