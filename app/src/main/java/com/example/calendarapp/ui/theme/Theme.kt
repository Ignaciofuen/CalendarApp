package com.example.calendarapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarapp.ui.viewmodel.ThemeViewModel

@Composable
fun CalendarAppTheme(
    // Inyectamos el ViewModel para que el tema sea reactivo
    themeViewModel: ThemeViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    // 1. Obtenemos el estado actual del tema (Colores, Fuentes, Bordes)
    val themeState by themeViewModel.currentTheme

    // 2. Esquema de Colores Dinámico
    val colorScheme = darkColorScheme(
        primary = themeState.accentColor, // Cambia según el selector de colores
        background = DefaultDarkBg,
        surface = DefaultSurface,
        onPrimary = DefaultDarkBg,
        onBackground = TextWhite,
        onSurface = TextWhite
    )

    // 3. Tipografía Dinámica
    // Usamos la función que creamos en Type.kt para obtener los estilos según la fuente elegida
    val dynamicTypography = getDynamicTypography(themeState.getFontFamily())

    // 4. Configuración de la Status Bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = themeState.systemBackgroundColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // 5. Aplicación del MaterialTheme Global
    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography, // <--- Aplica Minimal, Elegant, Geometric o Playful
        content = content
    )
}