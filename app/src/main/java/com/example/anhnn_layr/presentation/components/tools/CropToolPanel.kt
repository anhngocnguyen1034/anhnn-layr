package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton

data class CropPreset(
    val label: String,
    // null = tự do (giữ nguyên tỉ lệ khung khi phóng to/thu nhỏ)
    val aspect: Float?,
)

val CropPresets = listOf(
    CropPreset("Tự do", null),
    CropPreset("1:1", 1f),
    CropPreset("9:16", 9f / 16f),
    CropPreset("16:9", 16f / 9f),
    CropPreset("4:3", 4f / 3f),
    CropPreset("3:2", 3f / 2f),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CropToolPanel(
    selectedAspect: Float?,
    onSelectAspect: (Float?) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolPanelColumn(
        title = "Cắt ảnh",
        modifier = modifier,
    ) {
        ToolSectionLabel("Tỉ lệ khung")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CropPresets.forEach { preset ->
                FilterChip(
                    selected = preset.aspect == selectedAspect,
                    onClick = { onSelectAspect(preset.aspect) },
                    label = { Text(preset.label) },
                )
            }
        }

        Text(
            text = "Giữ trong khung để di chuyển • góc dưới-trái để xoay • " +
                "góc trên-phải để phóng to/thu nhỏ.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onReset) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Text("Đặt lại", modifier = Modifier.padding(start = 6.dp))
            }
            AnhnnGradientButton(
                text = "Áp dụng",
                onClick = onApply,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
