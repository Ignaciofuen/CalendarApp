package com.example.calendarapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.calendarapp.R

// ── GOOGLE FONTS PROVIDER ─────────────────────────────────────────────────────
// Utiliza el proveedor de fuentes de Google Play Services.
// Los certificados son proporcionados automáticamente por la librería ui-text-google-fonts.
val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// ── HELPER ────────────────────────────────────────────────────────────────────
private fun googleFontFamily(name: String): FontFamily {
    val font = GoogleFont(name)
    return FontFamily(
        Font(googleFont = font, fontProvider = GoogleFontProvider, weight = FontWeight.Light),
        Font(googleFont = font, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
        Font(googleFont = font, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
        Font(googleFont = font, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = font, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
        Font(googleFont = font, fontProvider = GoogleFontProvider, weight = FontWeight.ExtraBold),
    )
}

// ── ESTILOS 0-3: FUENTES DEL SISTEMA (sin internet) ──────────────────────────
val MinimalFont   = FontFamily.Default    // Roboto  — limpio y neutro
val ElegantFont   = FontFamily.Serif      // Noto Serif — clásico y refinado
val GeometricFont = FontFamily.SansSerif  // Sans-serif — moderno
val PlayfulFont   = FontFamily.Cursive    // Cursiva — divertido

// ── ESTILOS 4-7: GOOGLE FONTS (se descargan la primera vez) ──────────────────
val RoundedFont = googleFontFamily("Nunito")          // Redondeado, amigable
val ThinFont    = googleFontFamily("Raleway")         // Delgado, estilizado
val HeavyFont   = googleFontFamily("Oswald")          // Condensado y fuerte
val MonoFont    = googleFontFamily("JetBrains Mono")  // Monoespaciado, técnico

// ── GENERADOR DE TIPOGRAFÍA ───────────────────────────────────────────────────
fun getDynamicTypography(fontFamily: FontFamily): Typography {
    return Typography(
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}
