package com.example.anhnn_layr.presentation.components.tools

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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.viewmodels.EditorTool

private data class ToolTabItem(
    val tool: EditorTool,
    val label: String,
    val icon: ImageVector,
)

private val TOOL_TABS = listOf(
    ToolTabItem(EditorTool.BACKGROUND, "Nền", Icons.Outlined.FormatPaint),
    ToolTabItem(EditorTool.ERASE, "Cọ", Icons.Outlined.AutoFixHigh),
    ToolTabItem(EditorTool.EFFECTS, "FX", Icons.Outlined.AutoAwesome),
    ToolTabItem(EditorTool.CROP, "Cắt", Icons.Outlined.Crop),
    ToolTabItem(EditorTool.TEXT, "Chữ", Icons.Outlined.TextFields),
)

@Composable
fun ToolTabs(
    active: EditorTool,
    onSelect: (EditorTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        TOOL_TABS.forEach { item ->
            NavigationBarItem(
                selected = active == item.tool,
                onClick = { onSelect(item.tool) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
