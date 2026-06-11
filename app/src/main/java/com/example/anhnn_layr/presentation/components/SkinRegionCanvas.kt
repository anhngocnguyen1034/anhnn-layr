package com.example.anhnn_layr.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.anhnn_layr.utils.BrushMode
import com.example.anhnn_layr.utils.TouchPath

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
// Cỡ cọ quét vùng da = bề ngang bitmap * hệ số (chia thêm cho scale khi zoom).
private const val REGION_BRUSH_FRAC = 0.07f
// Màu hiển thị vùng đã quét (teal LAYR, mờ) — chỉ là lớp phủ xem trước, không bake.
private val REGION_COLOR = Color(0x5926C6B7)

/**
 * Lớp phủ "quét tay vùng da" đặt TRÊN ảnh preview trong tab Mặt: 1 ngón vẽ vùng
 * (commit [TouchPath] toạ độ bitmap qua [onCommitPath]), 2 ngón pinch zoom/pan như
 * [EraseCanvas]. Vùng đã quét + nét đang kéo hiển thị mờ màu teal để biết hiệu ứng
 * sẽ ăn vào đâu — kết quả thật do ViewModel render (mặt nạ vùng × SkinGate).
 */
@Composable
fun SkinRegionCanvas(
    committedPaths: List<TouchPath>,
    bitmapWidth: Int,
    bitmapHeight: Int,
    scale: Float,
    offset: Offset,
    onTransform: (newScale: Float, newOffset: Offset) -> Unit,
    onCommitPath: (TouchPath) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bmpW = bitmapWidth.toFloat()
    val bmpH = bitmapHeight.toFloat()
    var currentPath by remember { mutableStateOf<Path?>(null, neverEqualPolicy()) }
    var lastPoint by remember { mutableStateOf(Offset.Zero) }
    val currentPoints = remember { mutableListOf<Offset>() }
    var strokeBrushSize by remember { mutableStateOf(bmpW * REGION_BRUSH_FRAC) }

    // Path hiển thị các nét đã commit — dựng lại khi danh sách đổi.
    val committedDisplay = remember(committedPaths) {
        committedPaths.map { touch ->
            val p = Path()
            touch.points.firstOrNull()?.let { first ->
                p.moveTo(first.x, first.y)
                for (k in 1 until touch.points.size) {
                    val pt = touch.points[k]
                    p.lineTo(pt.x, pt.y)
                }
                if (touch.points.size == 1) p.lineTo(first.x + 0.01f, first.y)
            }
            p to touch.brushSize
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bmpW, bmpH, scale, offset) {
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
                    strokeBrushSize = (bmpW * REGION_BRUSH_FRAC / scale).coerceAtLeast(2f)
                    lastPoint = firstBmp
                    currentPath = Path().apply { moveTo(firstBmp.x, firstBmp.y) }
                    currentPoints.clear()
                    currentPoints.add(firstBmp)
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
                            onTransform(newScale, clampRegionPan(newOffset, newScale, canvasW, canvasH))
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
                            currentPoints.add(cur)
                            lastPoint = cur
                            ch.consume()
                        } else {
                            active.forEach { it.consume() }
                        }
                    }

                    if (brushStarted && currentPoints.isNotEmpty()) {
                        onCommitPath(
                            TouchPath(
                                points = currentPoints.toList(),
                                mode = BrushMode.PAINT,
                                brushSize = strokeBrushSize,
                                color = REGION_COLOR.toArgb(),
                            )
                        )
                    }
                    currentPath = null
                    currentPoints.clear()
                }
            },
    ) {
        val bmpToCanvas = size.width / bmpW
        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scale * bmpToCanvas, scale * bmpToCanvas, pivot = Offset.Zero)
        }) {
            committedDisplay.forEach { (path, width) ->
                drawPath(
                    path = path,
                    color = REGION_COLOR,
                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            currentPath?.let {
                drawPath(
                    path = it,
                    color = REGION_COLOR,
                    style = Stroke(width = strokeBrushSize, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

private fun clampRegionPan(offset: Offset, scale: Float, canvasW: Float, canvasH: Float): Offset {
    if (scale <= 1f) return Offset.Zero
    return Offset(
        x = offset.x.coerceIn(canvasW - canvasW * scale, 0f),
        y = offset.y.coerceIn(canvasH - canvasH * scale, 0f),
    )
}
