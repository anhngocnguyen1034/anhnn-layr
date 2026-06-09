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
    SLIM("Thon mặt"),
    SMOOTH("Mịn da"),
}

/**
 * Bảng "Chỉnh mặt": một thanh trượt cường độ cho mục đang chọn + dải thẻ mục
 * (giống [EffectsToolPanel]). Có "Mắt to", "Môi hồng", "Thon mặt" (V-line), "Mịn da".
 */
@Composable
fun FaceToolPanel(
    eyeEnlarge: Float,
    lipColor: Float,
    faceSlim: Float,
    skinSmooth: Float,
    faceDetected: Boolean?,
    onEyeEnlargeChange: (Float) -> Unit,
    onLipColorChange: (Float) -> Unit,
    onFaceSlimChange: (Float) -> Unit,
    onSkinSmoothChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noFace = faceDetected == false
    var selected by remember { mutableStateOf(FaceFeature.EYE) }

    val activeValue = when (selected) {
        FaceFeature.EYE -> eyeEnlarge
        FaceFeature.LIP -> lipColor
        FaceFeature.SLIM -> faceSlim
        FaceFeature.SMOOTH -> skinSmooth
    }
    val onActiveChange: (Float) -> Unit = when (selected) {
        FaceFeature.EYE -> onEyeEnlargeChange
        FaceFeature.LIP -> onLipColorChange
        FaceFeature.SLIM -> onFaceSlimChange
        FaceFeature.SMOOTH -> onSkinSmoothChange
    }

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
                label = FaceFeature.SLIM.label,
                value = formatFaceValue(faceSlim),
                selected = selected == FaceFeature.SLIM,
                onClick = { selected = FaceFeature.SLIM },
                enabled = !noFace,
            )
            ToolItemCard(
                label = FaceFeature.SMOOTH.label,
                value = formatFaceValue(skinSmooth),
                selected = selected == FaceFeature.SMOOTH,
                onClick = { selected = FaceFeature.SMOOTH },
                enabled = !noFace,
            )
        }
    }
}

// 0f..1f → 0..100 cho gọn mắt.
private fun formatFaceValue(value: Float): String = (value * 100f).roundToInt().toString()
