package com.example.anhnn_layr.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AnhnnPurpleLight,
    onPrimary = Color(0xFF171827),
    primaryContainer = Color(0xFF343676),
    onPrimaryContainer = Color(0xFFE9E9FF),
    secondary = Color(0xFFBAC0D8),
    onSecondary = Color(0xFF232533),
    tertiary = Pink80,
    background = Color(0xFF11131A),
    onBackground = Color(0xFFEDEEF6),
    surface = Color(0xFF191B24),
    onSurface = Color(0xFFEDEEF6),
    surfaceVariant = Color(0xFF282B36),
    onSurfaceVariant = Color(0xFFC5C8D6),
    outline = Color(0xFF747789),
    outlineVariant = Color(0xFF343744),
)

private val LightColorScheme = lightColorScheme(
    primary = AnhnnPurpleDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E9FF),
    onPrimaryContainer = Color(0xFF20215E),
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    tertiary = Pink40,
    onTertiary = Color.White,
    background = AnhnnBackground,
    onBackground = AnhnnInk,
    surface = AnhnnSurface,
    onSurface = AnhnnInk,
    surfaceVariant = AnhnnSurfaceVariant,
    onSurfaceVariant = AnhnnMuted,
    outline = Color(0xFF8B8EA0),
    outlineVariant = AnhnnOutline,
)

@Composable
fun AnhnnlayrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Tắt mặc định để giữ đúng nhận diện thương hiệu Anhnn (Purple Gradient)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
