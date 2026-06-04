package com.example.anhnn_layr.presentation.screens.layr

/**
 * Ảnh mẫu (online) dùng cho ảnh nền khu vực Hero.
 * Load qua Coil AsyncImage — cần quyền INTERNET (đã khai báo trong Manifest).
 *
 * Lưu ý: mục "ẢNH GẦN ĐÂY" KHÔNG dùng ảnh mẫu nữa — nó hiển thị 5 ảnh chụp
 * gần nhất thật từ thư viện (xem RembgViewModel.recentPhotos).
 */
object LayrSampleImages {
    // Ảnh nền lớn cho khu vực Hero
    const val HERO =
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&q=80"
}
