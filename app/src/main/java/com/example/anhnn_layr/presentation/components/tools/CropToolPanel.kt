package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Crop169
import androidx.compose.material.icons.outlined.Crop32
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.CropLandscape
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.AnhnnGradientButton

data class CropPreset(
    val label: String,
    // null = tự do (giữ nguyên tỉ lệ khung khi phóng to/thu nhỏ)
    val aspect: Float?,
    val icon: ImageVector,
)

val CropPresets = listOf(
    CropPreset("Tự do", null, Icons.Outlined.CropFree),
    CropPreset("1:1", 1f, Icons.Outlined.CropSquare),
    CropPreset("9:16", 9f / 16f, Icons.Outlined.CropPortrait),
    CropPreset("16:9", 16f / 9f, Icons.Outlined.Crop169),
    CropPreset("4:3", 4f / 3f, Icons.Outlined.CropLandscape),
    CropPreset("3:2", 3f / 2f, Icons.Outlined.Crop32),
)

@Composable
fun CropToolPanel(
    selectedAspect: Float?,
    onSelectAspect: (Float?) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolPanelColumn(title = "Cắt ảnh", modifier = modifier) {
        ToolItemStrip {
            CropPresets.forEach { preset ->
                ToolItemCard(
                    label = preset.label,
                    icon = preset.icon,
                    selected = preset.aspect == selectedAspect,
                    onClick = { onSelectAspect(preset.aspect) },
                )
            }
        }

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
