package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EffectsToolPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolPanelColumn(
        title = "Hiệu ứng",
        modifier = modifier,
        scrollable = true,
    ) {
        ToolSectionLabel("Chỉnh màu")
        ToolSliderRow(
            label = "Sáng",
            value = brightness,
            range = -1f..1f,
            suffix = "",
            onValueChange = onBrightnessChange,
        )
        ToolSliderRow(
            label = "Tương phản",
            value = contrast,
            range = -1f..1f,
            suffix = "",
            onValueChange = onContrastChange,
        )
        ToolSliderRow(
            label = "Bão hoà",
            value = saturation,
            range = -1f..1f,
            suffix = "",
            onValueChange = onSaturationChange,
        )
    }
}
