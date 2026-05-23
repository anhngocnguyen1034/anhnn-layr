package com.example.anhnn_layr.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.utils.SaveFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    selectedColor: Color,
    format: SaveFormat,
    onFormatChange: (SaveFormat) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTransparent = selectedColor == Color.Transparent

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Lưu ảnh", style = MaterialTheme.typography.titleMedium)
            Text(
                if (isTransparent)
                    "Nền trong suốt → xuất .png"
                else "Chọn định dạng file",
                style = MaterialTheme.typography.bodySmall,
            )

            if (!isTransparent) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = format == SaveFormat.PNG,
                        onClick = { onFormatChange(SaveFormat.PNG) },
                        label = { Text("PNG") },
                    )
                    FilterChip(
                        selected = format == SaveFormat.JPEG,
                        onClick = { onFormatChange(SaveFormat.JPEG) },
                        label = { Text("JPEG") },
                    )
                }
            }

            AnhnnGradientButton(
                text = "Lưu vào thư viện",
                onClick = onConfirm,
            )
        }
    }
}
