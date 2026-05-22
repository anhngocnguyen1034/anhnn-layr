package com.example.anhnn_layr.presentation.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private val DEFAULT_MODELS = listOf(
    "u2net", "u2netp", "u2net_human_seg", "silueta",
    "isnet-general-use", "birefnet-general", "birefnet-general-lite",
    "birefnet-portrait", "bria-rmbg",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    models: List<String> = DEFAULT_MODELS,
) {
    var expanded by remember { mutableStateOf(false) }
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            models.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m) },
                    onClick = {
                        onSelected(m)
                        expanded = false
                    },
                )
            }
        }
    }
}
