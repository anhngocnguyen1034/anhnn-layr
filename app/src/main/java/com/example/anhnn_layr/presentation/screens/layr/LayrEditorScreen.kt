package com.example.anhnn_layr.presentation.screens.layr

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * MÀN HÌNH EDITOR (Chỉnh sửa) của LAYR — màn CHỌN TÍNH NĂNG sau khi đã có ảnh.
 *
 * Đây là nơi các tính năng "Làm nét ảnh" và "Xóa nền" xuất hiện (theo yêu cầu:
 * tính năng chỉ hiện khi người dùng muốn chỉnh sửa ảnh).
 *
 * - Bấm "Xóa nền"   -> [onRemoveBackground] (chạy AI tách nền rồi mở trình sửa đầy đủ).
 * - Bấm "Làm nét ảnh" -> [onSharpen] (chạy AI upscale).
 *
 * @param imageUri ảnh người dùng vừa chọn/chụp, hiển thị để xem trước.
 */
@Composable
fun LayrEditorScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onSharpen: () -> Unit,
    onRemoveBackground: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LayrColors.Background)
            .statusBarsPadding(),
    ) {
        // --- Thanh tiêu đề: Quay lại / Chỉnh sửa ---
        EditorTopBar(onBack = onBack)

        // --- Ảnh xem trước (chiếm phần lớn màn hình) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(LayrColors.Surface),
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Ảnh đang chỉnh sửa",
                // Fit để hiển thị trọn ảnh, không phóng to cắt khuyết.
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
            )

            // Đường chia dọc trắng mờ (trang trí Before/After)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.005f)
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.6f)),
            )
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
        }

        // --- Thanh công cụ: 2 pill kính mờ chọn tính năng ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassPillButton(
                text = "Làm nét ảnh",
                icon = Icons.Outlined.AutoFixHigh,
                onClick = onSharpen,
                modifier = Modifier.weight(1f),
            )
            GlassPillButton(
                text = "Xóa nền",
                icon = Icons.Outlined.ContentCut,
                onClick = onRemoveBackground,
                modifier = Modifier.weight(1f),
            )
            GlassPillButton(
                text = "Chỉnh ảnh",
                icon = Icons.Outlined.Tune,
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            )
        }

        // --- Thanh điều hướng tính năng dưới cùng ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LayrColors.Background)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Đây là các lối tắt tính năng; bấm sẽ chạy AI tương ứng.
            LayrNavItem(
                text = "Làm nét ảnh",
                icon = Icons.Outlined.AutoFixHigh,
                selected = false,
                onClick = onSharpen,
            )
            LayrNavItem(
                text = "Xóa nền",
                icon = Icons.Outlined.ContentCut,
                selected = false,
                onClick = onRemoveBackground,
            )
            LayrNavItem(
                text = "Chỉnh ảnh",
                icon = Icons.Outlined.Tune,
                selected = false,
                onClick = onEdit,
            )
        }
    }
}

/** Thanh tiêu đề: nút quay lại + tiêu đề "Chỉnh sửa". */
@Composable
private fun EditorTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
            contentDescription = "Quay lại",
            tint = LayrColors.TextPrimary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .padding(12.dp),
        )
        Text(
            text = "Chỉnh sửa",
            color = LayrColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
