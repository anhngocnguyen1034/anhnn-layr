package com.example.anhnn_layr.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.example.anhnn_layr.utils.TouchPath

@Composable
fun EraseCanvas(
    workingBitmap: Bitmap,
    isEraseMode: Boolean,
    brushSize: Float,
    onCommitPath: (TouchPath) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var lastPoint by remember { mutableStateOf(Offset.Zero) }

    val bmpW = workingBitmap.width.toFloat()
    val bmpH = workingBitmap.height.toFloat()
    val imageBitmap = remember(workingBitmap) { workingBitmap.asImageBitmap() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(bmpW / bmpH),
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEraseMode, brushSize, bmpW, bmpH) {
                    detectDragGestures(
                        onDragStart = { screenPoint ->
                            val scale = bmpW / size.width
                            val p = Offset(screenPoint.x * scale, screenPoint.y * scale)
                            lastPoint = p
                            currentPath = Path().apply { moveTo(p.x, p.y) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val scale = bmpW / size.width
                            val cur = Offset(
                                change.position.x * scale,
                                change.position.y * scale,
                            )
                            val mid = Offset(
                                (lastPoint.x + cur.x) / 2f,
                                (lastPoint.y + cur.y) / 2f,
                            )
                            currentPath = currentPath?.apply {
                                quadraticBezierTo(lastPoint.x, lastPoint.y, mid.x, mid.y)
                            }
                            lastPoint = cur
                        },
                        onDragEnd = {
                            currentPath?.let { p ->
                                p.lineTo(lastPoint.x, lastPoint.y)
                                onCommitPath(TouchPath(p, isEraseMode, brushSize))
                            }
                            currentPath = null
                        },
                        onDragCancel = { currentPath = null },
                    )
                },
        ) {
            currentPath?.let { p ->
                val s = size.width / bmpW
                scale(s, s, pivot = Offset.Zero) {
                    drawPath(
                        path = p,
                        color = if (isEraseMode) Color(0x66FF1744) else Color(0x6600C853),
                        style = Stroke(
                            width = brushSize,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}
