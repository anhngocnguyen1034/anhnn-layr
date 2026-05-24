package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Viền sticker", style = MaterialTheme.typography.titleSmall)
        SliderRow(
            label = "Độ dày",
            value = outlineWidth,
            range = 0f..30f,
            suffix = "px",
            onValueChange = onOutlineWidthChange,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(OUTLINE_PRESETS) { preset ->
                val isSelected = preset.color.value == outlineColor.value
                val border = if (isSelected) 3.dp else 1.dp
                val borderColor = if (isSelected) Color(0xFF6A1B9A) else Color(0x33000000)
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

        Text("Bóng đổ", style = MaterialTheme.typography.titleSmall)
        SliderRow(
            label = "Độ mờ",
            value = shadowRadius,
            range = 0f..40f,
            suffix = "",
            onValueChange = onShadowRadiusChange,
        )

        Text("Chỉnh màu", style = MaterialTheme.typography.titleSmall)
        SliderRow(
            label = "Sáng",
            value = brightness,
            range = -1f..1f,
            suffix = "",
            onValueChange = onBrightnessChange,
        )
        SliderRow(
            label = "Tương phản",
            value = contrast,
            range = -1f..1f,
            suffix = "",
            onValueChange = onContrastChange,
        )
        SliderRow(
            label = "Bão hoà",
            value = saturation,
            range = -1f..1f,
            suffix = "",
            onValueChange = onSaturationChange,
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onValueChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            formatSliderValue(value, range) + suffix,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp),
        )
    }
}

private fun formatSliderValue(value: Float, range: ClosedFloatingPointRange<Float>): String {
    return if (range.start < 0f) {
        "%.1f".format(value)
    } else {
        value.toInt().toString()
    }
}
