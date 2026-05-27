package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.utils.OUTLINE_PRESETS

@Composable
fun EffectsToolPanel(
    outlineWidth: Float,
    outlineColor: Color,
    onOutlineWidthChange: (Float) -> Unit,
    onOutlineColorChange: (Color) -> Unit,
    shadowRadius: Float,
    onShadowRadiusChange: (Float) -> Unit,
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
        ToolSectionLabel("Viền sticker")
        ToolSliderRow(
            label = "Độ dày",
            value = outlineWidth,
            range = 0f..30f,
            suffix = " px",
            onValueChange = onOutlineWidthChange,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(OUTLINE_PRESETS) { preset ->
                val isSelected = preset.color.value == outlineColor.value
                val border = if (isSelected) 3.dp else 1.dp
                val borderColor = if (isSelected) AnhnnPurpleDark else Color(0x33000000)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(preset.color)
                        .border(border, borderColor, CircleShape)
                        .clickable { onOutlineColorChange(preset.color) },
                )
            }
        }

        ToolSectionLabel("Bóng đổ")
        ToolSliderRow(
            label = "Độ mờ",
            value = shadowRadius,
            range = 0f..40f,
            suffix = "",
            onValueChange = onShadowRadiusChange,
        )

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
