package com.example.anhnn_layr.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark

/**
 * Thanh kéo gọn cho bảng công cụ: track mảnh bo tròn, thumb tròn nhỏ, vùng chạm
 * thấp (28dp) nên không chiếm nhiều diện tích như [androidx.compose.material3.Slider].
 *
 * Với dải âm–dương (vd -1f..1f) đặt [bipolar] = true để phần active đổ màu **từ
 * giữa** ra hai phía, trực quan cho mức tăng/giảm.
 */
@Composable
fun CompactSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    enabled: Boolean = true,
    bipolar: Boolean = false,
    activeColor: Color = AnhnnPurpleDark,
    trackColor: Color = Color(0x33000000),
) {
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val frac = ((value.coerceIn(range.start, range.endInclusive) - range.start) / span)
        .coerceIn(0f, 1f)
    val effActive = if (enabled) activeColor else activeColor.copy(alpha = 0.3f)
    val effTrack = if (enabled) trackColor else trackColor.copy(alpha = 0.4f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(enabled, range) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = { onValueChangeFinished() },
                    onDragCancel = { onValueChangeFinished() },
                ) { change, _ ->
                    change.consume()
                    val f = (change.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(range.start + f * span)
                }
            }
            .pointerInput(enabled, range) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(range.start + f * span)
                    onValueChangeFinished()
                }
            },
    ) {
        val w = size.width
        val cy = size.height / 2
        val trackH = 3.dp.toPx()
        val thumbR = 8.dp.toPx()
        val thumbX = w * frac

        drawLine(
            color = effTrack,
            start = Offset(0f, cy),
            end = Offset(w, cy),
            strokeWidth = trackH,
            cap = StrokeCap.Round,
        )
        val activeStart = if (bipolar) w / 2f else 0f
        drawLine(
            color = effActive,
            start = Offset(activeStart, cy),
            end = Offset(thumbX, cy),
            strokeWidth = trackH,
            cap = StrokeCap.Round,
        )
        drawCircle(color = Color.White, radius = thumbR, center = Offset(thumbX, cy))
        drawCircle(
            color = Color.Black.copy(alpha = 0.18f),
            radius = thumbR,
            center = Offset(thumbX, cy),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
