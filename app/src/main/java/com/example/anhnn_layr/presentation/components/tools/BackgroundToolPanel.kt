package com.example.anhnn_layr.presentation.components.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.rememberCoroutineScope

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Màu nền", style = MaterialTheme.typography.titleSmall)
        BackgroundColorPicker(
            selected = selected,
            onSelected = onSelected,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            ) { Text(if (hasBackgroundImage) "Đổi ảnh nền" else "Ảnh nền từ thư viện") }
            if (hasBackgroundImage) {
                TextButton(onClick = { onBackgroundImageSelected(null) }) { Text("Bỏ") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mờ nền", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = backgroundBlur,
                onValueChange = onBackgroundBlurChange,
                valueRange = 0f..25f,
                enabled = hasBackgroundImage,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Text(
                backgroundBlur.toInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(28.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mịn viền", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = featherRadius,
                onValueChange = onFeatherChange,
                valueRange = 0f..20f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Text(
                "${featherRadius.toInt()} px",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(40.dp),
            )
        }
    }
}
