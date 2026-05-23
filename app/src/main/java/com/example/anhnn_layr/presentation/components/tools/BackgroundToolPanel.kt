package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.BackgroundColorPicker

@Composable
fun BackgroundToolPanel(
    selected: Color,
    onSelected: (Color) -> Unit,
    featherRadius: Float,
    onFeatherChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
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
