package com.example.anhnn_layr.presentation.components.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.components.CompactSlider
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleDark

// Chiều cao CỐ ĐỊNH của vùng công cụ dưới ảnh (bằng cỡ tab FX). Mọi tab dùng chung
// một chiều cao nên ảnh phía trên không bị nhảy/co khi đổi tab; nội dung dài hơn
// (vd tab Chữ) thì cuộn dọc bên trong.
private val ToolPanelHeight = 172.dp

@Composable
internal fun ToolPanelColumn(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(ToolPanelHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
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

/**
 * Dải thẻ "mục" cuộn ngang — mỗi mục là một [ToolItemCard]. Đây là khối tách bạch
 * các mục chỉnh sửa, thay cho việc dồn nhiều slider chồng nhau (giống app điện thoại).
 */
@Composable
internal fun ToolItemStrip(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

/**
 * Thẻ chọn một mục chỉnh sửa. Hiện [value] (số) nếu có, nếu không thì hiện [icon];
 * bên dưới là [label]. Mục đang chọn tô màu nhấn tím.
 */
@Composable
internal fun ToolItemCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        selected -> AnhnnPurpleDark
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        selected -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .width(64.dp)
            .clip(shape)
            .background(container)
            .then(
                if (selected || !enabled) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Thanh trượt gọn cho mục đang chọn (đặt phía trên dải thẻ). Reset [dragValue] theo
 * [resetKey] để thumb nhả về giá trị thật khi đổi mục. Nút ↺ tuỳ chọn để đặt lại.
 */
@Composable
internal fun ActiveSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    resetKey: Any,
    modifier: Modifier = Modifier,
    suffix: String = "",
    enabled: Boolean = true,
    showValue: Boolean = false,
    onReset: (() -> Unit)? = null,
) {
    var dragValue by remember(resetKey) { mutableStateOf<Float?>(null) }
    val clamped = value.coerceIn(range.start, range.endInclusive)
    val display = dragValue ?: clamped

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactSlider(
            value = display,
            range = range,
            onValueChange = {
                dragValue = it
                onValueChange(it)
            },
            onValueChangeFinished = { dragValue = null },
            enabled = enabled,
            bipolar = range.start < 0f,
            modifier = Modifier.weight(1f),
        )
        if (showValue) {
            Text(
                text = formatToolSliderValue(display, range) + suffix,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(50.dp)
                    .padding(start = 10.dp),
            )
        }
        if (onReset != null) {
            IconButton(onClick = onReset, enabled = enabled) {
                Icon(
                    Icons.Outlined.RestartAlt,
                    contentDescription = "Đặt lại",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun formatToolSliderValue(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
): String {
    return if (range.start < 0f) "%.1f".format(value) else value.toInt().toString()
}
