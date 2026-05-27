package com.example.anhnn_layr.presentation.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark

data class BgColorOption(val label: String, val color: Color, val transparent: Boolean = false)

val DEFAULT_BG_COLORS = listOf(
    BgColorOption("Trong suốt", Color.Transparent, transparent = true),
    BgColorOption("Trắng", Color.White),
    BgColorOption("Đen", Color.Black),
    BgColorOption("Đỏ", Color(0xFFE53935)),
    BgColorOption("Xanh lá", Color(0xFF43A047)),
    BgColorOption("Xanh dương", Color(0xFF1E88E5)),
    BgColorOption("Vàng", Color(0xFFFDD835)),
    BgColorOption("Hồng", Color(0xFFEC407A)),
    BgColorOption("Tím", Color(0xFF8E24AA)),
    BgColorOption("Cam", Color(0xFFFB8C00)),
)

@Composable
fun BackgroundColorPicker(
    selected: Color,
    onSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    options: List<BgColorOption> = DEFAULT_BG_COLORS,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
    ) {
        items(options) { opt ->
            val isSelected = opt.color.value == selected.value
            val borderColor = if (isSelected) AnhnnPurpleDark else Color(0x33000000)
            val borderWidth = if (isSelected) 3.dp else 1.dp
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .then(
                        if (opt.transparent) Modifier.checkerboardBackground(cellSize = 6.dp)
                        else Modifier.background(opt.color)
                    )
                    .border(borderWidth, borderColor, CircleShape)
                    .clickable { onSelected(opt.color) },
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
}
