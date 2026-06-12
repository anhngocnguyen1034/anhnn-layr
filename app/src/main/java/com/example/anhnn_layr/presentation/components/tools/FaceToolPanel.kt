package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark
import com.example.anhnn_layr.utils.LIP_PALETTE
import kotlin.math.roundToInt

private enum class FaceFeature(val label: String) {
    EYE("Mắt to"),
    LIP("Môi hồng"),
    TEETH("Răng trắng"),
    BLUSH("Má hồng"),
    SLIM("Thon mặt"),
    SMOOTH("Mịn da"),
    BRIGHTEN("Sáng da"),
    WARP("Nắn tay"),
}

// Bảng màu son (LIP_PALETTE ở utils) đổi sang Compose Color, tính 1 lần.
private val LIP_SHADES = LIP_PALETTE.map { Color(it) }

/**
 * Bảng "Chỉnh mặt": một thanh trượt cường độ cho mục đang chọn + dải thẻ mục
 * (giống [EffectsToolPanel]). Có "Mắt to", "Môi hồng", "Thon mặt" (V-line), "Mịn da".
 */
@Composable
fun FaceToolPanel(
    eyeEnlarge: Float,
    lipColor: Float,
    lipShade: Color,
    teethWhiten: Float,
    blush: Float,
    faceSlim: Float,
    skinSmooth: Float,
    skinBrighten: Float,
    autoApplied: Boolean,
    faceDetected: Boolean?,
    onAutoBeautyToggle: () -> Unit,
    onEyeEnlargeChange: (Float) -> Unit,
    onLipColorChange: (Float) -> Unit,
    onLipShadeChange: (Color) -> Unit,
    onTeethWhitenChange: (Float) -> Unit,
    onBlushChange: (Float) -> Unit,
    onFaceSlimChange: (Float) -> Unit,
    onSkinSmoothChange: (Float) -> Unit,
    onSkinBrightenChange: (Float) -> Unit,
    skinRegionEnabled: Boolean,
    hasSkinRegion: Boolean,
    onSkinRegionToggle: () -> Unit,
    onSkinRegionClear: () -> Unit,
    warpEnabled: Boolean,
    warpBrushSize: Float,
    warpCount: Int,
    onWarpModeChange: (Boolean) -> Unit,
    onWarpBrushSizeChange: (Float) -> Unit,
    onWarpUndo: () -> Unit,
    onWarpClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val noFace = faceDetected == false
    // Panel bị dựng lại mỗi lần đổi tab → khởi tạo theo chế độ nắn tay đang bật để
    // lớp FaceWarpCanvas trên ảnh và mục đang chọn không lệch nhau.
    var selected by remember { mutableStateOf(if (warpEnabled) FaceFeature.WARP else FaceFeature.EYE) }
    // Chọn mục nào thì đồng bộ luôn chế độ nắn tay (chỉ bật khi đang ở mục "Nắn tay").
    fun select(feature: FaceFeature) {
        selected = feature
        onWarpModeChange(feature == FaceFeature.WARP)
    }

    val activeValue = when (selected) {
        FaceFeature.EYE -> eyeEnlarge
        FaceFeature.LIP -> lipColor
        FaceFeature.TEETH -> teethWhiten
        FaceFeature.BLUSH -> blush
        FaceFeature.SLIM -> faceSlim
        FaceFeature.SMOOTH -> skinSmooth
        FaceFeature.BRIGHTEN -> skinBrighten
        FaceFeature.WARP -> warpBrushSize
    }
    val onActiveChange: (Float) -> Unit = when (selected) {
        FaceFeature.EYE -> onEyeEnlargeChange
        FaceFeature.LIP -> onLipColorChange
        FaceFeature.TEETH -> onTeethWhitenChange
        FaceFeature.BLUSH -> onBlushChange
        FaceFeature.SLIM -> onFaceSlimChange
        FaceFeature.SMOOTH -> onSkinSmoothChange
        FaceFeature.BRIGHTEN -> onSkinBrightenChange
        FaceFeature.WARP -> onWarpBrushSizeChange
    }

    ToolPanelColumn(title = "Chỉnh mặt", modifier = modifier) {
        ActiveSlider(
            value = activeValue,
            range = 0f..1f,
            onValueChange = onActiveChange,
            resetKey = selected,
            // Nắn tay không cần landmark nên vẫn dùng được khi không dò thấy mặt.
            enabled = !noFace || selected == FaceFeature.WARP,
            onReset = { onActiveChange(0f) },
        )
        if (noFace) {
            Text(
                text = "Không tìm thấy khuôn mặt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        // Bảng màu son — chỉ hiện khi đang chỉnh môi.
        if (selected == FaceFeature.LIP && !noFace) {
            LipShadeRow(selected = lipShade, onSelected = onLipShadeChange)
        }
        // Quét tay vùng da — chỉ hiện ở mục Mịn da / Sáng da: bật rồi quét ngón tay lên
        // vùng da trên ảnh để giới hạn hiệu ứng vào vùng đó (chưa quét = toàn mặt).
        if ((selected == FaceFeature.SMOOTH || selected == FaceFeature.BRIGHTEN) && !noFace) {
            SkinRegionRow(
                enabled = skinRegionEnabled,
                hasRegion = hasSkinRegion,
                onToggle = onSkinRegionToggle,
                onClear = onSkinRegionClear,
            )
        }
        // Nắn tay: gợi ý thao tác + hoàn tác/xoá nét. Slider phía trên chỉnh cỡ vùng kéo.
        if (selected == FaceFeature.WARP) {
            WarpRow(
                hasWarp = warpCount > 0,
                onUndo = onWarpUndo,
                onClear = onWarpClear,
            )
        }
        ToolItemStrip {
            // Làm đẹp 1 chạm: áp preset cả 6 mục; bấm lại khi đang đúng preset → về 0.
            ToolItemCard(
                label = "Tự động",
                icon = Icons.Outlined.AutoAwesome,
                selected = autoApplied,
                onClick = onAutoBeautyToggle,
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.EYE.label,
                value = formatFaceValue(eyeEnlarge),
                selected = selected == FaceFeature.EYE,
                onClick = { select(FaceFeature.EYE) },
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.LIP.label,
                value = formatFaceValue(lipColor),
                selected = selected == FaceFeature.LIP,
                onClick = { select(FaceFeature.LIP) },
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.TEETH.label,
                value = formatFaceValue(teethWhiten),
                selected = selected == FaceFeature.TEETH,
                onClick = { select(FaceFeature.TEETH) },
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.BLUSH.label,
                value = formatFaceValue(blush),
                selected = selected == FaceFeature.BLUSH,
                onClick = { select(FaceFeature.BLUSH) },
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.SLIM.label,
                value = formatFaceValue(faceSlim),
                selected = selected == FaceFeature.SLIM,
                onClick = { select(FaceFeature.SLIM) },
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.SMOOTH.label,
                value = formatFaceValue(skinSmooth),
                selected = selected == FaceFeature.SMOOTH,
                onClick = { select(FaceFeature.SMOOTH) },
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.BRIGHTEN.label,
                value = formatFaceValue(skinBrighten),
                selected = selected == FaceFeature.BRIGHTEN,
                onClick = { select(FaceFeature.BRIGHTEN) },
                enabled = !noFace,
            )
            // Nắn tay (liquify) kéo trực tiếp bằng ngón, không cần landmark → luôn bật.
            ToolItemCard(
                label = FaceFeature.WARP.label,
                value = if (warpCount > 0) warpCount.toString() else null,
                selected = selected == FaceFeature.WARP,
                onClick = { select(FaceFeature.WARP) },
            )
        }
    }
}

/** Hàng điều khiển nắn tay: gợi ý thao tác + nút hoàn tác nét cuối / xoá hết nét. */
@Composable
private fun WarpRow(
    hasWarp: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Vuốt ngón tay lên vùng cần nắn (vd: má → vuốt vào trong)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (hasWarp) {
            TextButton(onClick = onUndo) { Text("Hoàn tác") }
            TextButton(onClick = onClear) { Text("Xoá hết") }
        }
    }
}

/** Hàng điều khiển quét tay: chip bật/tắt chế độ quét + nút xoá vùng + gợi ý thao tác. */
@Composable
private fun SkinRegionRow(
    enabled: Boolean,
    hasRegion: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = enabled,
            onClick = onToggle,
            label = { Text("Quét tay") },
        )
        Text(
            text = when {
                enabled -> "Quét ngón tay lên vùng da cần chỉnh"
                hasRegion -> "Chỉ chỉnh vùng đã quét"
                else -> "Đang chỉnh toàn mặt"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (hasRegion) {
            TextButton(onClick = onClear) { Text("Xoá vùng") }
        }
    }
}

/** Dải chấm tròn chọn màu son (kiểu giống BackgroundColorPicker, cỡ nhỏ hơn). */
@Composable
private fun LipShadeRow(
    selected: Color,
    onSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    ) {
        items(LIP_SHADES) { shade ->
            val isSelected = shade.value == selected.value
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(shade)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) AnhnnPurpleDark else Color(0x33000000),
                        shape = CircleShape,
                    )
                    .clickable { onSelected(shade) },
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
}

// 0f..1f → 0..100 cho gọn mắt.
private fun formatFaceValue(value: Float): String = (value * 100f).roundToInt().toString()
