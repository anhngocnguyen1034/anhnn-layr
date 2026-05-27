package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class CropPreset(
    val label: String,
    val width: Int,
    val height: Int,
)

val CropPresets = listOf(
    CropPreset("1:1", 1, 1),
    CropPreset("9:16", 9, 16),
    CropPreset("16:9", 16, 9),
    CropPreset("4:3", 4, 3),
    CropPreset("3:2", 3, 2),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CropToolPanel(
    onCrop: (CropPreset) -> Unit,
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
                AssistChip(
                    onClick = { onCrop(preset) },
                    label = { Text(preset.label) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Crop, contentDescription = null)
                    },
                )
            }
        }
    }
}
