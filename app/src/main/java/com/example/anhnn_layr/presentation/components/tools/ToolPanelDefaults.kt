package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun ToolPanelColumn(
    title: String,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollModifier = if (scrollable) {
        Modifier
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState())
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(scrollModifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
        content()
    }
}

@Composable
internal fun ToolSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
internal fun ToolSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelWidth: Dp = 82.dp,
    valueWidth: Dp = 48.dp,
) {
    // Giá trị cục bộ trong lúc kéo: thumb bám ngón tay tức thì, không chờ vòng
    // cập nhật qua StateFlow → recompose → quay lại (vốn gây cảm giác khựng/trễ).
    // Khi thả tay (onValueChangeFinished) hoặc giá trị ngoài đổi thì nhả về prop.
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val clamped = value.coerceIn(range.start, range.endInclusive)
    val display = dragValue ?: clamped

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth),
        )
        Slider(
            value = display,
            onValueChange = {
                dragValue = it
                onValueChange(it)
            },
            onValueChangeFinished = { dragValue = null },
            valueRange = range,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = formatToolSliderValue(display, range) + suffix,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(valueWidth),
        )
    }
}

private fun formatToolSliderValue(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
): String {
    return if (range.start < 0f) "%.1f".format(value) else value.toInt().toString()
}
