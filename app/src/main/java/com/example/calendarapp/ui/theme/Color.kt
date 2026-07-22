package com.example.calendarapp.ui.theme

import androidx.compose.ui.graphics.Color

// 1. Tus colores base
val MintGreen = Color(0xFF00C853)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val TextWhite = Color(0xFFFFFFFF)  // <--- ¡Esta es la que te faltaba!
val TextGray = Color(0xFFB0B0B0)

// 2. Alias para los Defaults
val DefaultMint = MintGreen
val DefaultDarkBg = DarkBackground
val DefaultSurface = DarkSurface

// 3. Colores estándar de Material
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

// 4. --- FUNCIÓN EXTENSIÓN (IMPORTANTE) ---
// Esta función convierte los textos Hex "#RRGGBB" a colores reales.
// Sin esto, Theme.kt falla en las líneas 34-39.
fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Gray // Color de seguridad si falla
    }
}