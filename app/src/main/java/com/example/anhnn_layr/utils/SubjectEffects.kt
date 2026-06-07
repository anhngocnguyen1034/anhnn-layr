package com.example.anhnn_layr.utils

import android.graphics.ColorMatrix

/**
 * Ma trận màu 4x5 (20 float) cho brightness/contrast/saturation, hoặc null nếu
 * không chỉnh gì. Dùng chung cho ColorFilter (preview) và khi xuất ảnh.
 */
fun colorAdjustMatrixOrNull(brightness: Float, contrast: Float, saturation: Float): FloatArray? {
    if (brightness == 0f && contrast == 0f && saturation == 0f) return null
    return buildAdjustmentMatrix(brightness, contrast, saturation).array.copyOf()
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
