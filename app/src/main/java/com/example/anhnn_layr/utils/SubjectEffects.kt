package com.example.anhnn_layr.utils

import android.graphics.ColorMatrix

/**
 * Bộ lọc màu preset cho tab FX. Mỗi preset là một ma trận màu cố định; chỉnh tay
 * (sáng/tương phản/bão hoà) được nhân CHỒNG LÊN SAU nên người dùng tinh chỉnh tiếp
 * được trên nền filter. NONE = không lọc.
 */
enum class ColorPreset(val label: String) {
    NONE("Gốc"),
    VIVID("Tươi"),
    WARM("Ấm"),
    COOL("Lạnh"),
    ROSY("Hồng"),
    FILM("Phim"),
    SEPIA("Cổ điển"),
    MONO("Đen trắng");

    /** Ma trận của riêng preset (null với [NONE]). Mỗi lần gọi trả về bản mới. */
    fun matrixOrNull(): ColorMatrix? = when (this) {
        NONE -> null
        // Bão hoà + chút tương phản — màu "pop" kiểu chế độ Vivid.
        VIVID -> ColorMatrix().apply {
            setSaturation(1.3f)
            postConcat(contrastMatrix(1.08f))
        }
        WARM -> channelScaleMatrix(1.10f, 1.03f, 0.90f)
        COOL -> channelScaleMatrix(0.92f, 1.00f, 1.10f)
        ROSY -> channelScaleMatrix(1.08f, 0.97f, 1.04f)
        // Phim: hơi bạc màu + đen được "nâng" (lift) như film fade.
        FILM -> ColorMatrix().apply {
            setSaturation(0.85f)
            postConcat(
                ColorMatrix(floatArrayOf(
                    0.92f, 0f, 0f, 0f, 18f,
                    0f, 0.92f, 0f, 0f, 18f,
                    0f, 0f, 0.92f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f,
                )),
            )
        }
        // Ma trận sepia kinh điển.
        SEPIA -> ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ))
        MONO -> ColorMatrix().apply { setSaturation(0f) }
    }
}

/**
 * Ma trận màu 4x5 (20 float) hợp nhất: [preset] áp trước, brightness/contrast/saturation
 * chỉnh tay nhân chồng lên sau. Trả về null nếu không chỉnh gì (không cần ColorFilter).
 * Dùng chung cho ColorFilter (preview) và khi xuất ảnh — hai nơi PHẢI cùng một ma trận.
 */
fun colorAdjustMatrixOrNull(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    preset: ColorPreset = ColorPreset.NONE,
): FloatArray? {
    val presetMatrix = preset.matrixOrNull()
    if (presetMatrix == null && brightness == 0f && contrast == 0f && saturation == 0f) return null
    val m = presetMatrix ?: ColorMatrix()
    m.postConcat(buildAdjustmentMatrix(brightness, contrast, saturation))
    return m.array.copyOf()
}

private fun buildAdjustmentMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
    val satMatrix = ColorMatrix().apply { setSaturation((saturation + 1f).coerceIn(0f, 2f)) }
    val brightOff = brightness * 255f
    val brightMatrix = ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, brightOff,
        0f, 1f, 0f, 0f, brightOff,
        0f, 0f, 1f, 0f, brightOff,
        0f, 0f, 0f, 1f, 0f,
    ))
    return ColorMatrix().apply {
        postConcat(satMatrix)
        postConcat(contrastMatrix((contrast + 1f).coerceIn(0f, 2f)))
        postConcat(brightMatrix)
    }
}

/** Ma trận tương phản quanh trung điểm 127.5 với hệ số [c] (1 = giữ nguyên). */
private fun contrastMatrix(c: Float): ColorMatrix {
    val off = (1f - c) * 127.5f
    return ColorMatrix(floatArrayOf(
        c, 0f, 0f, 0f, off,
        0f, c, 0f, 0f, off,
        0f, 0f, c, 0f, off,
        0f, 0f, 0f, 1f, 0f,
    ))
}

/** Ma trận nhân riêng từng kênh R/G/B (đổi nhiệt độ/tông màu). */
private fun channelScaleMatrix(r: Float, g: Float, b: Float): ColorMatrix = ColorMatrix(
    floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ),
)
