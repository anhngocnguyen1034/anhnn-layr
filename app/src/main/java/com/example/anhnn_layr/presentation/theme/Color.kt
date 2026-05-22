package com.example.anhnn_layr.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Anhnn signature Purple Gradient
val AnhnnPurpleLight = Color(0xFFA1A2FF)
val AnhnnPurpleDark = Color(0xFF4B4EEE)

val AnhnnPurpleGradient: Brush
    get() = Brush.verticalGradient(colors = listOf(AnhnnPurpleLight, AnhnnPurpleDark))

// Material 3 fallbacks aligned to Anhnn brand
val Purple80 = AnhnnPurpleLight
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = AnhnnPurpleDark
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
