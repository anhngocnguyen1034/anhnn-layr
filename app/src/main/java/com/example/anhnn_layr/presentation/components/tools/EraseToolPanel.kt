package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.BackgroundColorPicker
import com.example.anhnn_layr.utils.BrushMode

private val BrushMode.label: String
    get() = when (this) {
        BrushMode.ERASE -> "Cọ xoá"
        BrushMode.RESTORE -> "Khôi phục"
        BrushMode.PAINT -> "Tô màu"
    }

private val BrushMode.icon: ImageVector
    get() = when (this) {
        BrushMode.ERASE -> Icons.Outlined.AutoFixHigh
        BrushMode.RESTORE -> Icons.Outlined.Healing
        BrushMode.PAINT -> Icons.Outlined.Brush
    }

@Composable
fun EraseToolPanel(
    brushMode: BrushMode,
    brushColor: Color,
    brushSize: Float,
    onModeChange: (BrushMode) -> Unit,
    onColorChange: (Color) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier,
    minBrush: Float = 10f,
    maxBrush: Float = 150f,
) {
    ToolPanelColumn(
        title = "Cọ chỉnh sửa",
        modifier = modifier,
        trailing = {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Hoàn tác")
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Làm lại")
            }
        },
    ) {
        // Cỡ cọ + chấm xem trước, luôn hiện ở trên.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                val previewSize = (brushSize / maxBrush * 32f).coerceAtLeast(4f).dp
                val previewColor = when (brushMode) {
                    BrushMode.ERASE -> Color(0xFFE53935)
                    BrushMode.RESTORE -> Color(0xFF43A047)
                    BrushMode.PAINT -> brushColor
                }
                Box(
                    modifier = Modifier
                        .size(previewSize)
                        .clip(CircleShape)
                        .background(previewColor),
                )
            }
            ActiveSlider(
                value = brushSize,
                range = minBrush..maxBrush,
                onValueChange = onBrushSizeChange,
                resetKey = "brush-size",
                suffix = " px",
                showValue = true,
                modifier = Modifier.weight(1f),
            )
        }

        if (brushMode == BrushMode.PAINT) {
            BackgroundColorPicker(
                selected = brushColor,
                onSelected = onColorChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        ToolItemStrip {
            BrushMode.entries.forEach { mode ->
                ToolItemCard(
                    label = mode.label,
                    icon = mode.icon,
                    selected = brushMode == mode,
                    onClick = { onModeChange(mode) },
                )
            }
        }
    }
}
