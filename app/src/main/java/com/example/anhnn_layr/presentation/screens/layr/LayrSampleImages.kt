package com.example.anhnn_layr.presentation.screens.layr

/**
 * Ảnh chân dung mẫu (online) dùng cho phần "ẢNH GẦN ĐÂY" và ảnh nền Hero.
 * Load qua Coil AsyncImage — cần quyền INTERNET (đã khai báo trong Manifest).
 * Thay bằng ảnh thật của người dùng khi đã có nguồn dữ liệu.
 */
object LayrSampleImages {
    // Ảnh nền lớn cho khu vực Hero
    const val HERO =
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&q=80"

    // Danh sách ảnh gần đây (chân dung)
    val RECENT = listOf(
        "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&q=80",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&q=80",
        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&q=80",
    )
}
