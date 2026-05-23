package com.example.anhnn_layr.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.checkerboardBackground(
    cellSize: Dp = 12.dp,
    light: Color = Color(0xFFFFFFFF),
    dark: Color = Color(0xFFCCCCCC),
): Modifier = this.drawBehind {
    val cell = cellSize.toPx()
    val cols = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val isDark = (x + y) % 2 == 1
            drawRect(
                color = if (isDark) dark else light,
                topLeft = Offset(x * cell, y * cell),
                size = Size(cell, cell),
            )
        }
    }
}
