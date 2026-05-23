package com.example.anhnn_layr.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.anhnn_layr.utils.TouchPath

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

@Composable
fun EraseCanvas(
    workingBitmap: Bitmap,
    originalBitmap: Bitmap,
    isEraseMode: Boolean,
    brushSize: Float,
    scale: Float,
    offset: Offset,
    onTransform: (newScale: Float, newOffset: Offset) -> Unit,
    onCommitPath: (TouchPath) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var lastPoint by remember { mutableStateOf(Offset.Zero) }

    val bmpW = workingBitmap.width.toFloat()
    val bmpH = workingBitmap.height.toFloat()
    val workingImage = remember(workingBitmap) { workingBitmap.asImageBitmap() }
    val originalImage = remember(originalBitmap) { originalBitmap.asImageBitmap() }
    val bmpIntSize = remember(workingBitmap) { IntSize(workingBitmap.width, workingBitmap.height) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(bmpW / bmpH)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(isEraseMode, brushSize, bmpW, bmpH, scale, offset) {
                awaitEachGesture {
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    var transforming = false
                    var brushStarted = false

                    fun screenToBitmap(p: Offset): Offset {
                        val contentX = (p.x - offset.x) / scale
                        val contentY = (p.y - offset.y) / scale
                        val s = bmpW / canvasW
                        return Offset(contentX * s, contentY * s)
                    }

                    val firstChange = awaitFirstDown(requireUnconsumed = false)
                    val firstBmp = screenToBitmap(firstChange.position)
                    lastPoint = firstBmp
                    currentPath = Path().apply { moveTo(firstBmp.x, firstBmp.y) }
                    brushStarted = true
                    firstChange.consume()

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val active = event.changes.filter { it.pressed }
                        if (active.isEmpty()) break

                        if (active.size >= 2) {
                            if (brushStarted) {
                                currentPath = null
                                brushStarted = false
                            }
                            transforming = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            val actualZoom = if (scale == 0f) 1f else newScale / scale
                            val newOffset = Offset(
                                x = offset.x + pan.x + (offset.x - centroid.x) * (actualZoom - 1f),
                                y = offset.y + pan.y + (offset.y - centroid.y) * (actualZoom - 1f),
                            )
                            val clamped = clampPan(newOffset, newScale, canvasW, canvasH)
                            onTransform(newScale, clamped)
                            active.forEach { it.consume() }
                        } else if (!transforming && brushStarted) {
                            val ch = active.first()
                            val cur = screenToBitmap(ch.position)
                            val mid = Offset(
                                (lastPoint.x + cur.x) / 2f,
                                (lastPoint.y + cur.y) / 2f,
                            )
                            currentPath = currentPath?.apply {
                                quadraticBezierTo(lastPoint.x, lastPoint.y, mid.x, mid.y)
                            }
                            lastPoint = cur
                            ch.consume()
                        } else {
                            active.forEach { it.consume() }
                        }
                    }

                    if (brushStarted) {
                        currentPath?.let { p ->
                            p.lineTo(lastPoint.x, lastPoint.y)
                            onCommitPath(TouchPath(p, isEraseMode, brushSize))
                        }
                    }
                    currentPath = null
                }
            },
    ) {
        val bmpToCanvas = size.width / bmpW
        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scale * bmpToCanvas, scale * bmpToCanvas, pivot = Offset.Zero)
        }) {
            drawImage(
                image = workingImage,
                srcOffset = IntOffset.Zero,
                srcSize = bmpIntSize,
                dstOffset = IntOffset.Zero,
                dstSize = bmpIntSize,
            )
            currentPath?.let { p ->
                if (isEraseMode) {
                    drawPath(
                        path = p,
                        color = Color.Black,
                        blendMode = BlendMode.Clear,
                        style = Stroke(
                            width = brushSize,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                } else {
                    val fillPath = Path()
                    strokeOutline(brushSize).getFillPath(p.asAndroidPath(), fillPath.asAndroidPath())
                    clipPath(fillPath) {
                        drawImage(
                            image = originalImage,
                            srcOffset = IntOffset.Zero,
                            srcSize = bmpIntSize,
                            dstOffset = IntOffset.Zero,
                            dstSize = bmpIntSize,
                        )
                    }
                }
            }
        }
    }
}

private fun strokeOutline(width: Float): android.graphics.Paint = android.graphics.Paint().apply {
    style = android.graphics.Paint.Style.STROKE
    strokeWidth = width
    strokeCap = android.graphics.Paint.Cap.ROUND
    strokeJoin = android.graphics.Paint.Join.ROUND
}

private fun clampPan(offset: Offset, scale: Float, canvasW: Float, canvasH: Float): Offset {
    if (scale <= 1f) return Offset.Zero
    val maxX = 0f
    val minX = canvasW - canvasW * scale
    val maxY = 0f
    val minY = canvasH - canvasH * scale
    return Offset(
        x = offset.x.coerceIn(minX, maxX),
        y = offset.y.coerceIn(minY, maxY),
    )
}
