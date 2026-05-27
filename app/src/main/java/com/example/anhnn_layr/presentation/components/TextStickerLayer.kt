package com.example.anhnn_layr.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.anhnn_layr.utils.TextSticker
import com.example.anhnn_layr.utils.drawTextStickers

@Composable
fun TextStickerLayer(
    stickers: List<TextSticker>,
    selectedId: String?,
    bitmapWidth: Int,
    bitmapHeight: Int,
    editable: Boolean,
    onTransform: (id: String, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = stickers.firstOrNull { it.id == selectedId }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (editable && selected != null) {
                    Modifier.pointerInput(selected.id, bitmapWidth, bitmapHeight) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            val bitmapPan = Offset(
                                x = pan.x * bitmapWidth / size.width.toFloat(),
                                y = pan.y * bitmapHeight / size.height.toFloat(),
                            )
                            onTransform(selected.id, bitmapPan, zoom, rotation)
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        drawContext.canvas.nativeCanvas.apply {
            save()
            scale(size.width / bitmapWidth, size.height / bitmapHeight)
            drawTextStickers(this, stickers)
            restore()
        }

        if (editable && selected != null) {
            val center = Offset(
                x = selected.center.x * size.width / bitmapWidth,
                y = selected.center.y * size.height / bitmapHeight,
            )
            val radius = (selected.fontSize * selected.scale * size.width / bitmapWidth)
                .coerceIn(28f, 180f)
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = radius,
                center = center,
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )
        }
    }
}
