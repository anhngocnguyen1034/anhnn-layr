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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.anhnn_layr.utils.WarpStroke
import kotlin.math.hypot

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
// Bán kính vùng ảnh hưởng = bề ngang bitmap * lerp(MIN, MAX, brushSize) (chia thêm
// cho scale khi zoom — zoom càng sâu nắn càng tinh, như cọ tẩy).
private const val WARP_RADIUS_MIN_FRAC = 0.05f
private const val WARP_RADIUS_MAX_FRAC = 0.18f
// Ngón tay phải dịch tối thiểu chừng này (px bitmap) mới phát một đoạn nắn —
// gom bớt sự kiện move dày đặc, đỡ dội recompose vô ích.
private const val MIN_SEGMENT_DIST = 3f
// Vòng tròn chỉ vùng ảnh hưởng quanh ngón tay (teal LAYR, chỉ hiển thị khi đang vuốt).
private val INDICATOR_COLOR = Color(0xB326C6B7)

/**
 * Lớp "nắn tay" (liquify) đặt TRÊN ảnh preview trong tab Mặt: 1 ngón vuốt lên ảnh →
 * mỗi đoạn dịch chuyển nhỏ phát một [WarpStroke] (toạ độ bitmap) qua [onWarpSegment]
 * NGAY KHI ĐANG VUỐT nên da kéo theo ngón tay tức thời (kênh conflated của ViewModel
 * tự gom khi render không kịp). 2 ngón pinch zoom/pan như [EraseCanvas].
 * Vòng tròn quanh ngón tay cho biết vùng sẽ bị kéo.
 */
@Composable
fun FaceWarpCanvas(
    bitmapWidth: Int,
    bitmapHeight: Int,
    brushSize: Float,
    scale: Float,
    offset: Offset,
    onTransform: (newScale: Float, newOffset: Offset) -> Unit,
    onWarpSegment: (stroke: WarpStroke, newGesture: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bmpW = bitmapWidth.toFloat()
    // Vị trí ngón tay (toạ độ bitmap) khi đang vuốt — null = không vẽ vòng chỉ vùng.
    var fingerPos by remember { mutableStateOf<Offset?>(null) }
    var warpRadius by remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bmpW, scale, offset, brushSize) {
                awaitEachGesture {
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    var transforming = false
                    var warping = false
                    var firstSegment = true

                    fun screenToBitmap(p: Offset): Offset {
                        val contentX = (p.x - offset.x) / scale
                        val contentY = (p.y - offset.y) / scale
                        val s = bmpW / canvasW
                        return Offset(contentX * s, contentY * s)
                    }

                    val firstChange = awaitFirstDown(requireUnconsumed = false)
                    var lastPoint = screenToBitmap(firstChange.position)
                    val radiusFrac = WARP_RADIUS_MIN_FRAC +
                        (WARP_RADIUS_MAX_FRAC - WARP_RADIUS_MIN_FRAC) * brushSize.coerceIn(0f, 1f)
                    warpRadius = (bmpW * radiusFrac / scale).coerceAtLeast(4f)
                    fingerPos = lastPoint
                    warping = true
                    firstChange.consume()

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val active = event.changes.filter { it.pressed }
                        if (active.isEmpty()) break

                        if (active.size >= 2) {
                            if (warping) {
                                warping = false
                                fingerPos = null
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
                            onTransform(newScale, clampWarpPan(newOffset, newScale, canvasW, canvasH))
                            active.forEach { it.consume() }
                        } else if (!transforming && warping) {
                            val ch = active.first()
                            val cur = screenToBitmap(ch.position)
                            fingerPos = cur
                            val dx = cur.x - lastPoint.x
                            val dy = cur.y - lastPoint.y
                            if (hypot(dx, dy) >= MIN_SEGMENT_DIST) {
                                onWarpSegment(
                                    WarpStroke(
                                        cx = lastPoint.x,
                                        cy = lastPoint.y,
                                        dx = dx,
                                        dy = dy,
                                        radius = warpRadius,
                                    ),
                                    firstSegment,
                                )
                                firstSegment = false
                                lastPoint = cur
                            }
                            ch.consume()
                        } else {
                            active.forEach { it.consume() }
                        }
                    }
                    fingerPos = null
                }
            },
    ) {
        // Vòng tròn chỉ vùng ảnh hưởng — vẽ trong cùng transform với ảnh để khớp vị trí.
        val pos = fingerPos ?: return@Canvas
        val bmpToCanvas = size.width / bmpW
        withTransform({
            translate(left = offset.x, top = offset.y)
            scale(scale * bmpToCanvas, scale * bmpToCanvas, pivot = Offset.Zero)
        }) {
            drawCircle(
                color = INDICATOR_COLOR,
                radius = warpRadius,
                center = pos,
                style = Stroke(width = 2f / (scale * bmpToCanvas)),
            )
        }
    }
}

private fun clampWarpPan(offset: Offset, scale: Float, canvasW: Float, canvasH: Float): Offset {
    if (scale <= 1f) return Offset.Zero
    return Offset(
        x = offset.x.coerceIn(canvasW - canvasW * scale, 0f),
        y = offset.y.coerceIn(canvasH - canvasH * scale, 0f),
    )
}
