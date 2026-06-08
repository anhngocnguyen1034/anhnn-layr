package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.LineWeight
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.BackgroundColorPicker
import com.example.anhnn_layr.presentation.components.BgColorOption
import com.example.anhnn_layr.utils.TextSticker
import com.example.anhnn_layr.utils.TextStickerFont

private val TEXT_COLORS = listOf(
    BgColorOption("Trắng", Color.White),
    BgColorOption("Đen", Color.Black),
    BgColorOption("Đỏ", Color(0xFFE53935)),
    BgColorOption("Xanh", Color(0xFF1E88E5)),
    BgColorOption("Vàng", Color(0xFFFDD835)),
    BgColorOption("Hồng", Color(0xFFEC407A)),
    BgColorOption("Tím", Color(0xFF8E24AA)),
)

private enum class TextSection(val label: String, val icon: ImageVector) {
    FONT("Font", Icons.Outlined.FontDownload),
    TEXT_COLOR("Màu chữ", Icons.Outlined.FormatColorText),
    OUTLINE_COLOR("Màu viền", Icons.Outlined.BorderColor),
    SIZE("Cỡ chữ", Icons.Outlined.FormatSize),
    OUTLINE("Viền", Icons.Outlined.LineWeight),
    SHADOW("Bóng", Icons.Outlined.Layers),
}

@Composable
fun TextToolPanel(
    stickers: List<TextSticker>,
    selectedId: String?,
    onAdd: () -> Unit,
    onSelect: (String) -> Unit,
    onFontChange: (TextStickerFont) -> Unit,
    onTextColorChange: (Color) -> Unit,
    onOutlineColorChange: (Color) -> Unit,
    onOutlineWidthChange: (Float) -> Unit,
    onShadowRadiusChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = stickers.firstOrNull { it.id == selectedId }
    var section by remember { mutableStateOf(TextSection.FONT) }

    ToolPanelColumn(
        title = "Chữ",
        modifier = modifier,
        trailing = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = "Thêm chữ")
            }
            IconButton(onClick = onDelete, enabled = selected != null) {
                Icon(Icons.Outlined.Delete, contentDescription = "Xoá chữ")
            }
        },
    ) {
        if (stickers.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(stickers) { sticker ->
                    FilterChip(
                        selected = sticker.id == selectedId,
                        onClick = { onSelect(sticker.id) },
                        label = { Text(sticker.text.ifBlank { "Chữ" }.take(12)) },
                    )
                }
            }
        } else {
            Text(
                text = "Chạm + để thêm chữ, rồi chạm vào chữ trên ảnh để sửa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Nội dung mục đang chọn ở trên.
        when (section) {
            TextSection.FONT -> FontDropdown(
                selected = selected?.font ?: TextStickerFont.INTER,
                enabled = selected != null,
                onSelected = onFontChange,
                modifier = Modifier.fillMaxWidth(),
            )
            TextSection.TEXT_COLOR -> BackgroundColorPicker(
                selected = selected?.textColor ?: Color.White,
                onSelected = onTextColorChange,
                options = TEXT_COLORS,
                modifier = Modifier.fillMaxWidth(),
            )
            TextSection.OUTLINE_COLOR -> BackgroundColorPicker(
                selected = selected?.outlineColor ?: Color.Black,
                onSelected = onOutlineColorChange,
                options = TEXT_COLORS,
                modifier = Modifier.fillMaxWidth(),
            )
            TextSection.SIZE -> ActiveSlider(
                value = selected?.fontSize ?: 72f,
                range = 24f..180f,
                onValueChange = onFontSizeChange,
                resetKey = section,
                suffix = " px",
                showValue = true,
                enabled = selected != null,
            )
            TextSection.OUTLINE -> ActiveSlider(
                value = selected?.outlineWidth ?: 0f,
                range = 0f..24f,
                onValueChange = onOutlineWidthChange,
                resetKey = section,
                suffix = " px",
                showValue = true,
                enabled = selected != null,
            )
            TextSection.SHADOW -> ActiveSlider(
                value = selected?.shadowRadius ?: 0f,
                range = 0f..32f,
                onValueChange = onShadowRadiusChange,
                resetKey = section,
                showValue = true,
                enabled = selected != null,
            )
        }

        ToolItemStrip {
            TextSection.entries.forEach { item ->
                ToolItemCard(
                    label = item.label,
                    icon = item.icon,
                    selected = section == item,
                    onClick = { section = item },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontDropdown(
    selected: TextStickerFont,
    enabled: Boolean,
    onSelected: (TextStickerFont) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (enabled) expanded = !expanded
        },
        modifier = modifier,
    ) {
        TextField(
            readOnly = true,
            enabled = enabled,
            value = selected.label,
            onValueChange = {},
            label = { Text("Font chữ") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = enabled),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TextStickerFont.entries.forEach { font ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = font.label,
                            fontFamily = FontFamily(Font(font.fontRes)),
                        )
                    },
                    onClick = {
                        onSelected(font)
                        expanded = false
                    },
                )
            }
        }
    }
}
