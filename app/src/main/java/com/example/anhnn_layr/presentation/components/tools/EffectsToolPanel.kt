package com.example.anhnn_layr.presentation.components.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.utils.ColorPreset
import com.example.anhnn_layr.utils.colorAdjustMatrixOrNull
import kotlin.math.roundToInt

private enum class EffectAdjustment(val label: String, val icon: ImageVector) {
    BRIGHTNESS("Sáng", Icons.Outlined.LightMode),
    CONTRAST("Tương phản", Icons.Outlined.Contrast),
    SATURATION("Bão hoà", Icons.Outlined.WaterDrop),
}

// Cạnh dài nhất của bitmap thumbnail filter (thu nhỏ 1 lần, 8 ô dùng chung).
private const val FILTER_THUMB_MAX = 96

@Composable
fun EffectsToolPanel(
    previewBitmap: Bitmap,
    colorPreset: ColorPreset,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onColorPresetChange: (ColorPreset) -> Unit,
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
        FilterPresetRow(
            previewBitmap = previewBitmap,
            selected = colorPreset,
            onSelected = onColorPresetChange,
        )
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

/**
 * Dải thumbnail bộ lọc: ảnh thật thu nhỏ (1 bitmap dùng chung cho cả 8 ô), mỗi ô áp
 * [ColorFilter] của preset đó trên GPU — không nướng thêm bitmap nào khi đổi filter.
 */
@Composable
private fun FilterPresetRow(
    previewBitmap: Bitmap,
    selected: ColorPreset,
    onSelected: (ColorPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Thu nhỏ 1 lần mỗi khi ảnh nguồn đổi (giữ tỉ lệ, cạnh dài = FILTER_THUMB_MAX).
    val thumb = remember(previewBitmap) {
        val scale = FILTER_THUMB_MAX.toFloat() / maxOf(previewBitmap.width, previewBitmap.height)
        if (scale >= 1f) previewBitmap.asImageBitmap()
        else Bitmap.createScaledBitmap(
            previewBitmap,
            (previewBitmap.width * scale).toInt().coerceAtLeast(1),
            (previewBitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        ).asImageBitmap()
    }
    val shape = RoundedCornerShape(10.dp)

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    ) {
        items(ColorPreset.entries) { preset ->
            val isSelected = preset == selected
            // Ma trận chỉ của preset (chỉnh tay không đưa vào thumbnail cho dễ so sánh).
            val filter = remember(preset) {
                colorAdjustMatrixOrNull(0f, 0f, 0f, preset)
                    ?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Image(
                    bitmap = thumb,
                    contentDescription = preset.label,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(shape)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) AnhnnPurpleDark else Color(0x22000000),
                            shape = shape,
                        )
                        .clickable { onSelected(preset) },
                    contentScale = ContentScale.Crop,
                    colorFilter = filter,
                )
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) AnhnnPurpleDark
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
