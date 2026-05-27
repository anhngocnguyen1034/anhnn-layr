package com.example.anhnn_layr.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class UpscaleModel(
    val id: String,
    val description: String,
)

private val UPSCALE_MODELS = listOf(
    UpscaleModel("RealESRGAN_x4plus", "Đa năng x4 (mặc định) — chất lượng tốt cho ảnh thực"),
    UpscaleModel("RealESRNet_x4plus", "Ít nhiễu hơn RealESRGAN nhưng chi tiết mượt hơn"),
    UpscaleModel("RealESRGAN_x4plus_anime_6B", "Tối ưu cho ảnh anime/illustration x4"),
    UpscaleModel("RealESRGAN_x2plus", "Phóng to x2 — nhanh, nhẹ máy"),
    UpscaleModel("realesr-animevideov3", "Anime video nhẹ — nhanh, phù hợp ảnh anime"),
    UpscaleModel("realesr-general-x4v3", "Tổng quát x4 nhẹ — nhanh, ít bộ nhớ"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpscaleModelDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedModel = UPSCALE_MODELS.firstOrNull { it.id == selected }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        TextField(
            readOnly = true,
            value = selected,
            onValueChange = {},
            label = { Text("Model") },
            supportingText = selectedModel?.let { { Text(it.description) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            UPSCALE_MODELS.forEach { m ->
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(m.id, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                m.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSelected(m.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
