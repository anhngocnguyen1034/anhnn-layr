package com.example.anhnn_layr.presentation.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Thanh kéo zoom kiểu máy ảnh (mô phỏng theo android-magnifying): track mảnh bo
 * tròn, phần active đổ màu nổi bật, các vạch chia kiểu thước (ruler ticks) và rung
 * nhẹ (haptic) mỗi khi kéo qua một vạch hoặc tap để nhảy tới vị trí.
 *
 * [value] trong khoảng 0f..1f (linear zoom).
 */
@Composable
fun CustomZoomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.3f),
    activeColor: Color = Color(0xFFFFD700), // vàng-gold nổi bật khi kéo
    thumbColor: Color = Color.White,
) {
    val view = LocalView.current
    val tickCount = 10

    Canvas(
        modifier = modifier
            .height(40.dp) // vùng chạm
            .pointerInput(Unit) {
                var lastStep: Int? = null
                // Kéo (drag) — rung nhẹ khi qua mỗi vạch
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                    val currentStep = (newValue * tickCount).toInt().coerceIn(0, tickCount)
                    if (lastStep != null && currentStep != lastStep) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    lastStep = currentStep
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                // Tap để nhảy tới vị trí
                detectTapGestures { offset ->
                    val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(newValue)
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            },
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        val trackHeight = 2.dp.toPx()
        val thumbRadius = 8.dp.toPx()
        val tickHeight = 6.dp.toPx()
        val thumbX = width * value

        // 1. Track nền (inactive)
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round,
        )

        // 2. Track hoạt động (0 -> vị trí hiện tại)
        drawLine(
            color = activeColor,
            start = Offset(0f, centerY),
            end = Offset(thumbX, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round,
        )

        // 3. Vạch chia kiểu thước (ruler) — đổi màu khi đã đi qua
        for (i in 0..tickCount) {
            val tickX = (width / tickCount) * i
            val tickColor = if (tickX <= thumbX) activeColor else trackColor
            drawLine(
                color = tickColor,
                start = Offset(tickX, centerY - tickHeight / 2),
                end = Offset(tickX, centerY + tickHeight / 2),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 4. Thumb tròn + viền mờ
        drawCircle(color = thumbColor, radius = thumbRadius, center = Offset(thumbX, centerY))
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = thumbRadius,
            center = Offset(thumbX, centerY),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
