package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

private enum class FaceFeature(val label: String) {
    EYE("Mắt to"),
    LIP("Môi hồng"),
}

/**
 * Bảng "Chỉnh mặt": một thanh trượt cường độ cho mục đang chọn + dải thẻ mục
 * (giống [EffectsToolPanel]). Đợt này có "Mắt to" và "Môi hồng"; "Cằm thon" để sẵn
 * dạng khoá cho lần mở rộng sau.
 */
@Composable
fun FaceToolPanel(
    eyeEnlarge: Float,
    lipColor: Float,
    faceDetected: Boolean?,
    onEyeEnlargeChange: (Float) -> Unit,
    onLipColorChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noFace = faceDetected == false
    var selected by remember { mutableStateOf(FaceFeature.EYE) }

    val activeValue = if (selected == FaceFeature.EYE) eyeEnlarge else lipColor
    val onActiveChange: (Float) -> Unit =
        if (selected == FaceFeature.EYE) onEyeEnlargeChange else onLipColorChange

    ToolPanelColumn(title = "Chỉnh mặt", modifier = modifier) {
        ActiveSlider(
            value = activeValue,
            range = 0f..1f,
            onValueChange = onActiveChange,
            resetKey = selected,
            enabled = !noFace,
            onReset = { onActiveChange(0f) },
        )
        if (noFace) {
            Text(
                text = "Không tìm thấy khuôn mặt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        ToolItemStrip {
            ToolItemCard(
                label = FaceFeature.EYE.label,
                value = formatFaceValue(eyeEnlarge),
                selected = selected == FaceFeature.EYE,
                onClick = { selected = FaceFeature.EYE },
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.LIP.label,
                value = formatFaceValue(lipColor),
                selected = selected == FaceFeature.LIP,
                onClick = { selected = FaceFeature.LIP },
                enabled = !noFace,
            )
            ToolItemCard(
                label = "Cằm thon",
                value = "0",
                selected = false,
                onClick = {},
                enabled = false,
            )
        }
    }
}

// 0f..1f → 0..100 cho gọn mắt.
private fun formatFaceValue(value: Float): String = (value * 100f).roundToInt().toString()
