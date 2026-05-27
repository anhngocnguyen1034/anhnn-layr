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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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

    ToolPanelColumn(
        title = "Nền",
        modifier = modifier,
    ) {
        ToolSectionLabel("Màu nền")
        BackgroundColorPicker(
            selected = selected,
            onSelected = onSelected,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onUseOriginalBackground) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
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
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (hasBackgroundImage) "Đổi nền" else "Thư viện")
            }
            if (hasBackgroundImage) {
                TextButton(onClick = { onBackgroundImageSelected(null) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bỏ")
                }
            }
        }
        ToolSliderRow(
            label = "Mờ nền",
            value = backgroundBlur,
            range = 0f..25f,
            suffix = "",
            enabled = hasBackgroundImage,
            onValueChange = onBackgroundBlurChange,
        )
        ToolSliderRow(
            label = "Mịn viền",
            value = featherRadius,
            range = 0f..20f,
            suffix = " px",
            onValueChange = onFeatherChange,
        )
    }
}
