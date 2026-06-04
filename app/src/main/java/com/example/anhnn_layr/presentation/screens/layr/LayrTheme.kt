package com.example.anhnn_layr.presentation.screens.layr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Bảng màu cho bộ giao diện "LAYR" (Dark mode + điểm nhấn teal).
 * Tách riêng khỏi theme Purple của Anhnn để dễ tùy biến độc lập.
 */
object LayrColors {
    // --- Nền & bề mặt (theo feat.md) ---
    val Background = Color(0xFF121212)      // Nền charcoal siêu tối
    val Surface = Color(0xFF222222)         // Thẻ card / pill tạo chiều sâu
    val SurfaceVariant = Color(0xFF2C2C2E)  // Vòng tròn icon, viền nhẹ
    val Outline = Color(0xFF3A3A3C)         // Viền phân tách

    // --- Điểm nhấn teal ---
    val Teal = Color(0xFF5EEAD4)            // Accent chính (LƯU, Xem tất cả, icon)
    val TealContainer = Color(0xFFAEE9DD)   // Nền tab đang chọn (teal nhạt)
    val OnTealContainer = Color(0xFF0E3B34) // Chữ/icon trên nền teal nhạt

    // --- Chữ ---
    val TextPrimary = Color(0xFFF5F5F7)     // Tiêu đề, nội dung chính
    val TextSecondary = Color(0xFF9A9AA0)   // Mô tả phụ
    val TextMuted = Color(0xFF6B6B70)       // Nhãn mờ
}

/**
 * Hiệu ứng Glassmorphism (kính mờ).
 *
 * minSdk 24 không hỗ trợ blur "xuyên nền" ổn định trên mọi máy, nên ta mô phỏng
 * bằng lớp phủ trắng bán trong suốt + viền sáng mảnh — cho cảm giác kính mờ nổi
 * trên ảnh nền. Tăng [alpha] để kính "đục" hơn.
 */
fun Modifier.glass(
    shape: Shape,
    alpha: Float = 1f,
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f * alpha),
                Color.White.copy(alpha = 0.06f * alpha),
            ),
        ),
    )
    .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
