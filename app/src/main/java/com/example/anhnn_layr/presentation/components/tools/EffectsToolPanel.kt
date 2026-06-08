package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.roundToInt

private enum class EffectAdjustment(val label: String, val icon: ImageVector) {
    BRIGHTNESS("Sáng", Icons.Outlined.LightMode),
    CONTRAST("Tương phản", Icons.Outlined.Contrast),
    SATURATION("Bão hoà", Icons.Outlined.WaterDrop),
}

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
    // Mỗi lúc chỉ 1 thanh trượt cho mục đang chọn → bảng gọn, tách bạch từng mục.
    var selected by remember { mutableStateOf(EffectAdjustment.BRIGHTNESS) }

    val activeValue = when (selected) {
        EffectAdjustment.BRIGHTNESS -> brightness
        EffectAdjustment.CONTRAST -> contrast
        EffectAdjustment.SATURATION -> saturation
    }
    val onActiveChange: (Float) -> Unit = when (selected) {
        EffectAdjustment.BRIGHTNESS -> onBrightnessChange
        EffectAdjustment.CONTRAST -> onContrastChange
        EffectAdjustment.SATURATION -> onSaturationChange
    }

    ToolPanelColumn(title = "Hiệu ứng", modifier = modifier) {
        ActiveSlider(
            value = activeValue,
            range = -1f..1f,
            onValueChange = onActiveChange,
            resetKey = selected,
            onReset = { onActiveChange(0f) },
        )
        ToolItemStrip {
            EffectAdjustment.entries.forEach { item ->
                val v = when (item) {
                    EffectAdjustment.BRIGHTNESS -> brightness
                    EffectAdjustment.CONTRAST -> contrast
                    EffectAdjustment.SATURATION -> saturation
                }
                ToolItemCard(
                    label = item.label,
                    value = formatEffectValue(v),
                    selected = selected == item,
                    onClick = { selected = item },
                )
            }
        }
    }
}

// Hiển thị -1f..1f thành phần trăm có dấu cho gọn: 0, +40, -20.
private fun formatEffectValue(value: Float): String {
    val pct = (value * 100f).roundToInt()
    return if (pct > 0) "+$pct" else pct.toString()
}
