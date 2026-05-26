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

data class RembgModel(
    val id: String,
    val description: String,
)

private val DEFAULT_MODELS = listOf(
    RembgModel("isnet-general-use", "Đa năng, chi tiết viền tốt hơn u2net (mặc định)"),
    RembgModel("u2net", "Đa năng, cân bằng tốc độ & chất lượng"),
    RembgModel("u2netp", "Phiên bản nhẹ của u2net — nhanh hơn, nhẹ máy"),
    RembgModel("u2net_human_seg", "Tối ưu cho ảnh người (chân dung, cơ thể)"),
    RembgModel("silueta", "Giống u2net nhưng dung lượng nhỏ hơn nhiều"),
    RembgModel("birefnet-general", "Chất lượng cao, tách viền sắc nét (chậm hơn)"),
    RembgModel("birefnet-general-lite", "Bản nhẹ của birefnet — nhanh hơn, chất lượng khá"),
    RembgModel("birefnet-portrait", "Chuyên cho ảnh chân dung, tóc/viền mịn"),
    RembgModel("bria-rmbg", "Mô hình thương mại Bria, chất lượng cao cho ảnh sản phẩm"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    models: List<RembgModel> = DEFAULT_MODELS,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedModel = models.firstOrNull { it.id == selected }
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
            models.forEach { m ->
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
