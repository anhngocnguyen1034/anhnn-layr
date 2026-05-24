package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.viewmodels.EditorTool

@Composable
fun ToolTabs(
    active: EditorTool,
    onSelect: (EditorTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = active == EditorTool.BACKGROUND,
            onClick = { onSelect(EditorTool.BACKGROUND) },
            icon = { Icon(Icons.Outlined.FormatPaint, contentDescription = null) },
            label = { Text("Nền") },
        )
        NavigationBarItem(
            selected = active == EditorTool.ERASE,
            onClick = { onSelect(EditorTool.ERASE) },
            icon = { Icon(Icons.Outlined.AutoFixHigh, contentDescription = null) },
            label = { Text("Cọ") },
        )
        NavigationBarItem(
            selected = active == EditorTool.EFFECTS,
            onClick = { onSelect(EditorTool.EFFECTS) },
            icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
            label = { Text("FX") },
        )
        NavigationBarItem(
            selected = active == EditorTool.CROP,
            onClick = { onSelect(EditorTool.CROP) },
            icon = { Icon(Icons.Outlined.Crop, contentDescription = null) },
            label = { Text("Cắt") },
            enabled = false,
        )
        NavigationBarItem(
            selected = active == EditorTool.TEXT,
            onClick = { onSelect(EditorTool.TEXT) },
            icon = { Icon(Icons.Outlined.TextFields, contentDescription = null) },
            label = { Text("Chữ") },
            enabled = false,
        )
    }
}

@Composable
fun ComingSoonPanel(name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Tính năng $name đang phát triển.", style = MaterialTheme.typography.bodyMedium)
    }
}
