package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EraseToolPanel(
    isEraseMode: Boolean,
    brushSize: Float,
    onModeChange: (Boolean) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = isEraseMode,
                onClick = { onModeChange(true) },
                label = { Text("Cọ xoá") },
            )
            FilterChip(
                selected = !isEraseMode,
                onClick = { onModeChange(false) },
                label = { Text("Khôi phục") },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                val previewSize = (brushSize / maxBrush * 32f).coerceAtLeast(4f).dp
                Box(
                    modifier = Modifier
                        .size(previewSize)
                        .clip(CircleShape)
                        .background(
                            if (isEraseMode) Color(0xFFE53935) else Color(0xFF43A047)
                        ),
                )
            }
            Text(
                text = "Kích cỡ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp),
            )
            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChange,
                valueRange = minBrush..maxBrush,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${brushSize.toInt()} px",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(52.dp)
                    .padding(start = 8.dp),
            )
        }
    }
}
