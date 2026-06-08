package com.example.anhnn_layr.presentation.components.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.BackgroundColorPicker
import com.example.anhnn_layr.utils.decodeBackgroundBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class BgSection(val label: String) {
    COLOR("Màu nền"),
    IMAGE("Ảnh nền"),
    BLUR("Mờ nền"),
    FEATHER("Mịn viền"),
}

@Composable
fun BackgroundToolPanel(
    selected: Color,
    onSelected: (Color) -> Unit,
    featherRadius: Float,
    onFeatherChange: (Float) -> Unit,
    hasBackgroundImage: Boolean,
    backgroundBlur: Float,
    onBackgroundImageSelected: (Bitmap?) -> Unit,
    onBackgroundBlurChange: (Float) -> Unit,
    onUseOriginalBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf(BgSection.COLOR) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { decodeBackgroundBitmap(ctx, uri) }
                onBackgroundImageSelected(bmp)
            }
        }
    }

    ToolPanelColumn(title = "Nền", modifier = modifier) {
        // Nội dung mục đang chọn ở trên, dải thẻ ngăn cách bên dưới.
        when (section) {
            BgSection.COLOR -> BackgroundColorPicker(
                selected = selected,
                onSelected = onSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            BgSection.IMAGE -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onUseOriginalBackground) {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ảnh gốc")
                }
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasBackgroundImage) "Đổi nền" else "Thư viện")
                }
                if (hasBackgroundImage) {
                    TextButton(onClick = { onBackgroundImageSelected(null) }) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bỏ")
                    }
                }
            }
            BgSection.BLUR -> {
                if (!hasBackgroundImage) {
                    Text(
                        text = "Chọn ảnh nền ở mục \"Ảnh nền\" để làm mờ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ActiveSlider(
                    value = backgroundBlur,
                    range = 0f..25f,
                    onValueChange = onBackgroundBlurChange,
                    resetKey = section,
                    showValue = true,
                    enabled = hasBackgroundImage,
                )
            }
            BgSection.FEATHER -> ActiveSlider(
                value = featherRadius,
                range = 0f..20f,
                onValueChange = onFeatherChange,
                resetKey = section,
                suffix = " px",
                showValue = true,
            )
        }

        ToolItemStrip {
            ToolItemCard(
                label = BgSection.COLOR.label,
                icon = Icons.Outlined.Palette,
                selected = section == BgSection.COLOR,
                onClick = { section = BgSection.COLOR },
            )
            ToolItemCard(
                label = BgSection.IMAGE.label,
                icon = Icons.Outlined.Wallpaper,
                selected = section == BgSection.IMAGE,
                onClick = { section = BgSection.IMAGE },
            )
            ToolItemCard(
                label = BgSection.BLUR.label,
                icon = Icons.Outlined.BlurOn,
                selected = section == BgSection.BLUR,
                onClick = { section = BgSection.BLUR },
            )
            ToolItemCard(
                label = BgSection.FEATHER.label,
                icon = Icons.Outlined.Gradient,
                selected = section == BgSection.FEATHER,
                onClick = { section = BgSection.FEATHER },
            )
        }
    }
}
