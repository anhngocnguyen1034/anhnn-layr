package com.example.anhnn_layr.presentation.screens.layr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * MÀN HÌNH HOME của LAYR — theo Luồng tuyến tính (Linear Flow) trong feat.md.
 *
 * Home chỉ để NHẬP ẢNH (chụp / chọn thư viện / ảnh gần đây) rồi chuyển sang Editor.
 * Các tính năng (Làm nét / Xóa nền) nằm ở màn Editor, KHÔNG nằm ở Home.
 *
 * Toàn bộ callback được hoist ra ngoài (stateless). Thanh điều hướng dưới cùng
 * (Bottom Navigation) do [LayrMainScreen] quản lý để cố định xuyên các tab.
 */
@Composable
fun LayrHomeScreen(
    onCapture: () -> Unit,
    onPickFromGallery: () -> Unit,
    onSeeAllRecent: () -> Unit,
    onOpenRecent: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onTipPortrait: () -> Unit,
    onTipRemoveBg: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LayrColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // === Thành phần 1: Top App Bar ===
        LayrTopBar(onOpenSettings = onOpenSettings)

        // === Thành phần 2: Hero — khu vực nhập ảnh chính ===
        HeroSection(
            onCapture = onCapture,
            onPickFromGallery = onPickFromGallery,
        )

        // === Thành phần 3: ẢNH GẦN ĐÂY ===
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LayrSectionHeader(
                title = "ẢNH GẦN ĐÂY",
                actionText = "Xem tất cả",
                onActionClick = onSeeAllRecent,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp), // spacing 8dp theo feat.md
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                items(LayrSampleImages.RECENT) { url ->
                    RecentPhotoCard(
                        imageUrl = url,
                        onClick = { onOpenRecent(LayrSampleImages.RECENT.indexOf(url)) },
                    )
                }
            }
        }

        // === Thành phần 4: MẸO NHANH ===
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LayrSectionHeader(title = "MẸO NHANH")
            TipCard(
                title = "Tối ưu chân dung",
                description = "Sử dụng AI để làm mịn da mà vẫn giữ chi tiết lỗ chân lông.",
                icon = Icons.Outlined.SentimentSatisfied, // mặt cười cho mẹo làm nét
                onClick = onTipPortrait,
            )
            TipCard(
                title = "Xóa nền nhanh",
                description = "Tách chủ thể khỏi nền chỉ với một chạm bằng AI.",
                icon = Icons.Outlined.ContentCut, // cây kéo cho mẹo xóa nền
                onClick = onTipRemoveBg,
            )
        }
    }
}

/** Thanh tiêu đề trên cùng: tên app LAYR + icon ✨, bên phải nút cài đặt. */
@Composable
private fun LayrTopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LAYR",
                color = LayrColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = LayrColors.Teal,
                modifier = Modifier.size(20.dp),
            )
        }
        // Nút cài đặt / tài khoản dạng tròn
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LayrColors.Surface)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Cài đặt",
                tint = LayrColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Khu vực Hero: ảnh chân dung lớn làm nền + đường chia dọc giả lập Before/After
 * (mang tính trang trí), phủ nút kính mờ "CHỤP ẢNH NGAY" + link "Chọn từ thư viện".
 */
@Composable
private fun HeroSection(
    onCapture: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f) // chiếm ~40% màn hình phía trên
            .clip(RoundedCornerShape(24.dp))
            .background(LayrColors.Surface),
    ) {
        // Ảnh nền
        AsyncImage(
            model = LayrSampleImages.HERO,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )

        // Lớp scrim tối nhẹ để nút nổi bật
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
                ),
        )

        // Đường chia dọc trắng mờ (trang trí Before/After)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.004f)
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.55f)),
        )

        // Nhãn góc
        CornerTag(
            text = "BEFORE",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
        )
        CornerTag(
            text = "AFTER",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
        )

        // Lớp phủ trung tâm: nút chụp ảnh + link thư viện
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlassPillButton(
                text = "CHỤP ẢNH NGAY",
                icon = Icons.Rounded.CameraAlt,
                onClick = onCapture,
                tint = Color.White,
            )
            Text(
                text = "Chọn từ thư viện",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onPickFromGallery)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, heightDp = 900)
@Composable
private fun LayrHomeScreenPreview() {
    LayrHomeScreen(
        onCapture = {},
        onPickFromGallery = {},
        onSeeAllRecent = {},
        onOpenRecent = {},
        onOpenSettings = {},
        onTipPortrait = {},
        onTipRemoveBg = {},
    )
}
