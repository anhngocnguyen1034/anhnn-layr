package com.example.anhnn_layr.presentation.screens.layr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.anhnn_layr.domain.models.DraftSummary
import com.example.anhnn_layr.presentation.components.DraftsRow

/**
 * MÀN STUDIO — "Dự án của tôi": danh sách bản nháp đã lưu.
 * Tái sử dụng [DraftsRow] sẵn có của dự án.
 */
@Composable
fun StudioScreen(
    drafts: List<DraftSummary>,
    onOpenDraft: (String) -> Unit,
    onDeleteDraft: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LayrColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Dự án của tôi",
            color = LayrColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )

        if (drafts.isEmpty()) {
            EmptyStudio()
        } else {
            DraftsRow(
                drafts = drafts,
                onOpen = onOpenDraft,
                onDelete = onDeleteDraft,
            )
        }
    }
}

/** Trạng thái trống khi chưa có bản nháp nào. */
@Composable
private fun EmptyStudio() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = LayrColors.TextMuted,
        )
        Text(
            text = "Chưa có dự án nào",
            color = LayrColors.TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Ảnh bạn chỉnh sửa và lưu nháp sẽ hiện ở đây.",
            color = LayrColors.TextMuted,
            fontSize = 13.sp,
        )
    }
}
