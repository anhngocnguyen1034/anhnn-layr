package com.example.anhnn_layr.presentation.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.anhnn_layr.utils.TextSticker
import com.example.anhnn_layr.utils.drawTextStickers
import com.example.anhnn_layr.utils.loadTypeface
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val HANDLE_HIT_RADIUS = 60f
private const val HANDLE_DRAW_RADIUS = 14f
private const val HANDLE_OFFSET = 56f
private const val BOX_PADDING = 16f

@Composable
fun TextStickerLayer(
    stickers: List<TextSticker>,
    selectedId: String?,
    bitmapWidth: Int,
    bitmapHeight: Int,
    editable: Boolean,
    onTransform: (id: String, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onStartEdit: (id: String) -> Unit = {},
    onTapEmpty: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val selected = stickers.firstOrNull { it.id == selectedId }
    val context = LocalContext.current

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (editable) {
                    // Chạm vào chữ để sửa nội dung ngay trên ảnh; chạm chỗ trống để kết thúc.
                    Modifier.pointerInput(stickers, bitmapWidth, bitmapHeight) {
                        detectTapGestures { pos ->
                            val id = hitTestSticker(
                                context = context,
                                stickers = stickers,
                                pos = pos,
                                viewW = size.width.toFloat(),
                                viewH = size.height.toFloat(),
                                bitmapW = bitmapWidth,
                                bitmapH = bitmapHeight,
                            )
                            if (id != null) onStartEdit(id) else onTapEmpty()
                        }
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (editable && selected != null) {
                    Modifier
                        .pointerInput(selected.id, bitmapWidth, bitmapHeight, selected.rotation, selected.scale, selected.fontSize, selected.text, selected.font, selected.center) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val box = measureLocalBox(context, selected)
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                val centerView = bitmapToView(selected.center, viewW, viewH, bitmapWidth, bitmapHeight)
                                val handleLocal = Offset(0f, -box.height / 2f - HANDLE_OFFSET)
                                val handleView = transformLocalToView(
                                    local = handleLocal,
                                    sticker = selected,
                                    viewW = viewW,
                                    viewH = viewH,
                                    bitmapW = bitmapWidth,
                                    bitmapH = bitmapHeight,
                                )
                                val onHandle = hypot(
                                    (down.position.x - handleView.x).toDouble(),
                                    (down.position.y - handleView.y).toDouble(),
                                ).toFloat() <= HANDLE_HIT_RADIUS
                                if (!onHandle) return@awaitEachGesture
                                down.consume()
                                var lastAngle = atan2(
                                    (down.position.y - centerView.y).toDouble(),
                                    (down.position.x - centerView.x).toDouble(),
                                )
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val angle = atan2(
                                        (change.position.y - centerView.y).toDouble(),
                                        (change.position.x - centerView.x).toDouble(),
                                    )
                                    val deltaDeg = Math.toDegrees(angle - lastAngle).toFloat()
                                    lastAngle = angle
                                    if (deltaDeg != 0f) {
                                        onTransform(selected.id, Offset.Zero, 1f, deltaDeg)
                                    }
                                    change.consume()
                                }
                            }
                        }
                        .pointerInput(selected.id, bitmapWidth, bitmapHeight) {
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
            drawTextStickers(context, this, stickers)
            restore()
        }

        if (editable && selected != null) {
            drawSelectionFrame(
                sticker = selected,
                box = measureLocalBox(context, selected),
                bitmapW = bitmapWidth,
                bitmapH = bitmapHeight,
            )
        }
    }
}

private fun DrawScope.drawSelectionFrame(
    sticker: TextSticker,
    box: Size,
    bitmapW: Int,
    bitmapH: Int,
) {
    val halfW = box.width / 2f
    val halfH = box.height / 2f
    val corners = listOf(
        Offset(-halfW, -halfH),
        Offset(halfW, -halfH),
        Offset(halfW, halfH),
        Offset(-halfW, halfH),
    ).map { transformLocalToView(it, sticker, size.width, size.height, bitmapW, bitmapH) }

    val frameColor = Color.White.copy(alpha = 0.9f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    val stroke = Stroke(width = 2.5f, cap = StrokeCap.Round, pathEffect = dash)
    for (i in corners.indices) {
        val a = corners[i]
        val b = corners[(i + 1) % corners.size]
        drawLine(color = frameColor, start = a, end = b, strokeWidth = stroke.width, cap = stroke.cap, pathEffect = dash)
    }

    corners.forEach { c ->
        drawCircle(color = Color.White, radius = 8f, center = c)
        drawCircle(
            color = Color(0xFF4B4EEE),
            radius = 8f,
            center = c,
            style = Stroke(width = 2f),
        )
    }

    val handleLocal = Offset(0f, -halfH - HANDLE_OFFSET)
    val handleView = transformLocalToView(handleLocal, sticker, size.width, size.height, bitmapW, bitmapH)
    val topCenter = transformLocalToView(Offset(0f, -halfH), sticker, size.width, size.height, bitmapW, bitmapH)
    drawLine(
        color = frameColor,
        start = topCenter,
        end = handleView,
        strokeWidth = 2f,
        pathEffect = dash,
    )
    drawCircle(color = Color(0xFF4B4EEE), radius = HANDLE_DRAW_RADIUS, center = handleView)
    drawCircle(
        color = Color.White,
        radius = HANDLE_DRAW_RADIUS,
        center = handleView,
        style = Stroke(width = 3f),
    )
    drawCircle(
        color = Color.White,
        radius = HANDLE_DRAW_RADIUS - 5f,
        center = handleView,
        style = Stroke(width = 2f),
    )
}

private fun measureLocalBox(
    context: android.content.Context,
    sticker: TextSticker,
): Size {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textSize = sticker.fontSize
        typeface = sticker.font.loadTypeface(context)
    }
    val lines = sticker.text.lines().ifEmpty { listOf(" ") }
    val maxWidth = lines.maxOf { paint.measureText(it.ifEmpty { " " }) }
    val lineHeight = paint.fontSpacing
    val height = lineHeight * lines.size
    val pad = BOX_PADDING + sticker.outlineWidth
    return Size(maxWidth + pad * 2f, height + pad * 2f)
}

/** Trả về id của chữ (trên cùng) chứa điểm chạm [pos] (toạ độ view), hoặc null. */
private fun hitTestSticker(
    context: android.content.Context,
    stickers: List<TextSticker>,
    pos: Offset,
    viewW: Float,
    viewH: Float,
    bitmapW: Int,
    bitmapH: Int,
): String? {
    if (viewW <= 0f || viewH <= 0f) return null
    // Duyệt từ trên xuống (vẽ sau nằm trên).
    for (sticker in stickers.asReversed()) {
        val box = measureLocalBox(context, sticker)
        val bx = pos.x * bitmapW / viewW
        val by = pos.y * bitmapH / viewH
        val dx = bx - sticker.center.x
        val dy = by - sticker.center.y
        val rad = Math.toRadians(-sticker.rotation.toDouble())
        val cos = cos(rad).toFloat()
        val sin = sin(rad).toFloat()
        val rx = dx * cos - dy * sin
        val ry = dx * sin + dy * cos
        val scale = sticker.scale.coerceAtLeast(0.0001f)
        val lx = rx / scale
        val ly = ry / scale
        if (kotlin.math.abs(lx) <= box.width / 2f && kotlin.math.abs(ly) <= box.height / 2f) {
            return sticker.id
        }
    }
    return null
}

private fun bitmapToView(
    pt: Offset,
    viewW: Float,
    viewH: Float,
    bitmapW: Int,
    bitmapH: Int,
): Offset = Offset(pt.x * viewW / bitmapW, pt.y * viewH / bitmapH)

private fun transformLocalToView(
    local: Offset,
    sticker: TextSticker,
    viewW: Float,
    viewH: Float,
    bitmapW: Int,
    bitmapH: Int,
): Offset {
    val scaled = Offset(local.x * sticker.scale, local.y * sticker.scale)
    val rad = Math.toRadians(sticker.rotation.toDouble())
    val cos = cos(rad).toFloat()
    val sin = sin(rad).toFloat()
    val rotated = Offset(
        x = scaled.x * cos - scaled.y * sin,
        y = scaled.x * sin + scaled.y * cos,
    )
    val bitmapPt = Offset(sticker.center.x + rotated.x, sticker.center.y + rotated.y)
    return bitmapToView(bitmapPt, viewW, viewH, bitmapW, bitmapH)
}
