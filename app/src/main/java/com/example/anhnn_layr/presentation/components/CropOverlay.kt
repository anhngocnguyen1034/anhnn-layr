package com.example.anhnn_layr.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import com.example.anhnn_layr.utils.CropFrame
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val HANDLE_HIT = 64f
private const val HANDLE_RADIUS = 15f
private const val MIN_CROP = 32f // pixel bitmap nhỏ nhất của khung

private val FramePurple = Color(0xFF4B4EEE)

private enum class CropMode { MOVE, RESIZE, ROTATE }

/**
 * Lớp phủ khung cắt tương tác trên ảnh. Khung được định nghĩa theo toạ độ pixel của
 * bitmap ([CropFrame]); overlay quy đổi sang pixel hiển thị bằng tỉ lệ đồng nhất vì
 * khung xem trước đã khớp đúng tỉ lệ ảnh.
 *
 * - Giữ trong khung rồi kéo → di chuyển khung.
 * - Góc dưới-trái → giữ và xoay khung theo ngón tay.
 * - Góc trên-phải → kéo để phóng to / thu nhỏ khung.
 */
@Composable
fun CropOverlay(
    frame: CropFrame,
    bitmapWidth: Int,
    bitmapHeight: Int,
    onFrameChange: (CropFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestFrame = rememberUpdatedState(frame)

    Canvas(
        modifier = modifier.pointerInput(bitmapWidth, bitmapHeight) {
            val s = size.width.toFloat() / bitmapWidth.coerceAtLeast(1)
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val f0 = latestFrame.value
                val centerPx = Offset(f0.cx * s, f0.cy * s)
                val trPx = cornerPx(f0, signX = 1f, signY = -1f, s = s)
                val blPx = cornerPx(f0, signX = -1f, signY = 1f, s = s)

                val mode = when {
                    down.position.distanceTo(trPx) <= HANDLE_HIT -> CropMode.RESIZE
                    down.position.distanceTo(blPx) <= HANDLE_HIT -> CropMode.ROTATE
                    isInsideFrame(down.position, f0, s) -> CropMode.MOVE
                    else -> null
                } ?: return@awaitEachGesture

                down.consume()
                val width0 = f0.width
                val height0 = f0.height
                val rot0 = f0.rotationDeg
                val dist0 = down.position.distanceTo(centerPx).coerceAtLeast(1f)
                val angle0 = angleTo(down.position, centerPx)
                val maxDim = maxOf(bitmapWidth, bitmapHeight) * 2f
                var lastPos = down.position

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    when (mode) {
                        CropMode.MOVE -> {
                            val delta = change.position - lastPos
                            val cur = latestFrame.value
                            onFrameChange(
                                cur.copy(
                                    cx = (cur.cx + delta.x / s).coerceIn(0f, bitmapWidth.toFloat()),
                                    cy = (cur.cy + delta.y / s).coerceIn(0f, bitmapHeight.toFloat()),
                                ),
                            )
                        }
                        CropMode.RESIZE -> {
                            val factor = change.position.distanceTo(centerPx) / dist0
                            onFrameChange(
                                latestFrame.value.copy(
                                    width = (width0 * factor).coerceIn(MIN_CROP, maxDim),
                                    height = (height0 * factor).coerceIn(MIN_CROP, maxDim),
                                ),
                            )
                        }
                        CropMode.ROTATE -> {
                            val deltaDeg = Math.toDegrees(
                                (angleTo(change.position, centerPx) - angle0).toDouble(),
                            ).toFloat()
                            onFrameChange(latestFrame.value.copy(rotationDeg = rot0 + deltaDeg))
                        }
                    }
                    lastPos = change.position
                    change.consume()
                }
            }
        },
    ) {
        val s = size.width / bitmapWidth.coerceAtLeast(1)
        val tl = cornerPx(frame, -1f, -1f, s)
        val tr = cornerPx(frame, 1f, -1f, s)
        val br = cornerPx(frame, 1f, 1f, s)
        val bl = cornerPx(frame, -1f, 1f, s)

        // Làm mờ phần ngoài khung.
        val path = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }
        clipPath(path, clipOp = ClipOp.Difference) {
            drawRect(color = Color.Black.copy(alpha = 0.45f), size = size)
        }

        // Đường lưới 1/3 trong khung.
        val gridColor = Color.White.copy(alpha = 0.4f)
        for (i in 1..2) {
            val t = i / 3f
            drawLine(gridColor, lerp(tl, bl, t), lerp(tr, br, t), strokeWidth = 1f)
            drawLine(gridColor, lerp(tl, tr, t), lerp(bl, br, t), strokeWidth = 1f)
        }

        // Viền khung.
        val border = Color.White.copy(alpha = 0.95f)
        val corners = listOf(tl, tr, br, bl)
        for (i in corners.indices) {
            drawLine(
                color = border,
                start = corners[i],
                end = corners[(i + 1) % corners.size],
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }

        // Tay nắm: góc trên-phải = phóng to/thu nhỏ; góc dưới-trái = xoay.
        drawResizeHandle(tr)
        drawRotateHandle(bl)
    }
}

private fun DrawScope.drawResizeHandle(center: Offset) {
    drawCircle(color = Color.White, radius = HANDLE_RADIUS, center = center)
    drawCircle(color = FramePurple, radius = HANDLE_RADIUS, center = center, style = Stroke(width = 3f))
    // Mũi tên chéo gợi ý kéo to/nhỏ.
    val a = HANDLE_RADIUS * 0.45f
    drawLine(FramePurple, center + Offset(-a, -a), center + Offset(a, a), strokeWidth = 3f, cap = StrokeCap.Round)
}

private fun DrawScope.drawRotateHandle(center: Offset) {
    drawCircle(color = FramePurple, radius = HANDLE_RADIUS, center = center)
    drawCircle(color = Color.White, radius = HANDLE_RADIUS, center = center, style = Stroke(width = 3f))
    // Cung tròn gợi ý xoay.
    val r = HANDLE_RADIUS * 0.5f
    drawCircle(
        color = Color.White,
        radius = r,
        center = center,
        style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)),
    )
}

/** Toạ độ pixel hiển thị của một góc khung (signX/signY = ±1). */
private fun cornerPx(frame: CropFrame, signX: Float, signY: Float, s: Float): Offset {
    val lx = signX * frame.width * s / 2f
    val ly = signY * frame.height * s / 2f
    val rad = Math.toRadians(frame.rotationDeg.toDouble())
    val c = cos(rad).toFloat()
    val sn = sin(rad).toFloat()
    return Offset(
        x = frame.cx * s + (lx * c - ly * sn),
        y = frame.cy * s + (lx * sn + ly * c),
    )
}

private fun isInsideFrame(point: Offset, frame: CropFrame, s: Float): Boolean {
    val dx = point.x - frame.cx * s
    val dy = point.y - frame.cy * s
    val rad = Math.toRadians(-frame.rotationDeg.toDouble())
    val c = cos(rad).toFloat()
    val sn = sin(rad).toFloat()
    val lx = dx * c - dy * sn
    val ly = dx * sn + dy * c
    return abs(lx) <= frame.width * s / 2f && abs(ly) <= frame.height * s / 2f
}

private fun Offset.distanceTo(other: Offset): Float =
    hypot((x - other.x).toDouble(), (y - other.y).toDouble()).toFloat()

private fun angleTo(point: Offset, center: Offset): Float =
    kotlin.math.atan2((point.y - center.y).toDouble(), (point.x - center.x).toDouble()).toFloat()

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
