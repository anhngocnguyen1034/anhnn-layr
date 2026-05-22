package com.example.anhnn_layr.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.anhnn_layr.presentation.theme.AnhnnPurpleGradient
import com.example.anhnn_layr.presentation.theme.AnhnnlayrTheme

@Composable
fun AnhnnGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(AnhnnPurpleGradient)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun AnhnnGradientButtonPreviewLight() {
    AnhnnlayrTheme(darkTheme = false) {
        AnhnnGradientButton(text = "Chọn ảnh", onClick = {})
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun AnhnnGradientButtonPreviewDark() {
    AnhnnlayrTheme(darkTheme = true) {
        AnhnnGradientButton(text = "Chọn ảnh", onClick = {})
    }
}
