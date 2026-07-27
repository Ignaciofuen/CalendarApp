package com.example.calendarapp.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.ui.theme.*
import com.example.calendarapp.widget.MonthCalendarWidget
import com.example.calendarapp.widget.MonthCalendarWidgetReceiver
import com.example.calendarapp.widget.TodayCalendarWidget
import com.example.calendarapp.widget.TodayCalendarWidgetReceiver
import com.example.calendarapp.widget.WeekCalendarWidget
import com.example.calendarapp.widget.WeekCalendarWidgetReceiver
import com.example.calendarapp.widget.UpcomingEventsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

enum class DayMarkerStyle {
    CLASSIC, SQUARE, SQUARE_FILLED, SPHERE_3D, MINIMAL, INSET
}

@Immutable
data class AppTheme(
    val accentColor: Color = Color(0xFF00E676),
    val markerStyle: DayMarkerStyle = DayMarkerStyle.CLASSIC,
    val borderStyle: Int = 1,
    val typographyStyle: Int = 0,
    val globalBackgroundImage: String? = null,
    val backgroundImages: Map<Int, String?> = mutableMapOf(),
    val backgroundOpacity: Float = 0.3f,
    val cardOpacity: Float = 1.0f,
    val blurAmount: Float = 0f,
    val calendarTextColor: Color = Color.White,
    val calendarTextBrightness: Float = 1.0f,
    val isCustomTextActive: Boolean = false,
    val systemBackgroundColor: Color = Color(0xFF0A0C0B),
    val weekendColor: Color = Color(0xFFFF5252),
    val secondaryTextColor: Color = Color.Gray,
    val globalTextScale: Float = 1.0f,
    val showBackgroundImages: Boolean = true,
    val syncLabelsWithAccent: Boolean = false,
    val syncMainTextWithAccent: Boolean = false,
    val bgRotation: Float = 0f,
    val bgOffsetX: Float = 0f,
    val bgOffsetY: Float = 0f,
    val bgScale: Float = 1f,
    val textShadowIntensity: Float = 0f
) {
    fun getLabelColor(): Color = if (syncLabelsWithAccent) accentColor else secondaryTextColor
    fun getMainTextColor(): Color = if (syncMainTextWithAccent) accentColor else calendarTextColor
    // Color de card/surface ligeramente más claro que el fondo del sistema
    fun getCardColor(): Color = systemBackgroundColor.copy(
        red = (systemBackgroundColor.red + 0.07f).coerceAtMost(1f),
        green = (systemBackgroundColor.green + 0.07f).coerceAtMost(1f),
        blue = (systemBackgroundColor.blue + 0.07f).coerceAtMost(1f)
    )

    fun getFontFamily(): FontFamily = when (typographyStyle) {
        0 -> MinimalFont
        1 -> ElegantFont
        2 -> GeometricFont
        3 -> PlayfulFont
        4 -> RoundedFont
        5 -> ThinFont
        6 -> HeavyFont
        7 -> MonoFont
        else -> MinimalFont
    }

    fun getBorderRadius(): Dp = when (borderStyle) {
        0 -> 0.dp
        1 -> 8.dp
        2 -> 16.dp
        3 -> 28.dp
        4 -> 100.dp
        else -> 12.dp
    }
}

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)
    
    // INICIALIZACIÓN DIRECTA: Cargamos las preferencias antes de crear el estado
    // Esto asegura que Compose vea el estado correcto desde el primer fotograma
    private val _currentTheme = mutableStateOf(loadThemeFromPrefs())
    val currentTheme: State<AppTheme> = _currentTheme

    private fun loadThemeFromPrefs(): AppTheme {
        val savedImages = mutableMapOf<Int, String?>()
        for (i in 1..12) {
            val uri = prefs.getString("bg_image_month_$i", null)
            if (!uri.isNullOrEmpty()) savedImages[i] = uri
        }

        return AppTheme(
            accentColor = Color(prefs.getInt("accentColor", Color(0xFF00E676).toArgb())),
            markerStyle = DayMarkerStyle.valueOf(prefs.getString("markerStyle", DayMarkerStyle.CLASSIC.name) ?: DayMarkerStyle.CLASSIC.name),
            borderStyle = prefs.getInt("borderStyle", 1),
            typographyStyle = prefs.getInt("typographyStyle", 0),
            globalBackgroundImage = prefs.getString("globalBackgroundImage", null),
            backgroundImages = savedImages,
            backgroundOpacity = prefs.getFloat("backgroundOpacity", 0.3f),
            cardOpacity = prefs.getFloat("cardOpacity", 1.0f),
            blurAmount = prefs.getFloat("blurAmount", 0f),
            calendarTextColor = Color(prefs.getInt("calendarTextColor", Color.White.toArgb())),
            calendarTextBrightness = prefs.getFloat("calendarTextBrightness", 1.0f),
            isCustomTextActive = prefs.getBoolean("isCustomTextActive", false),
            systemBackgroundColor = Color(prefs.getInt("systemBackgroundColor", Color(0xFF0A0C0B).toArgb())),
            weekendColor = Color(prefs.getInt("weekendColor", Color(0xFFFF5252).toArgb())),
            secondaryTextColor = Color(prefs.getInt("secondaryTextColor", Color.Gray.toArgb())),
            globalTextScale = prefs.getFloat("globalTextScale", 1.0f),
            showBackgroundImages = prefs.getBoolean("showBackgroundImages", true),
            syncLabelsWithAccent = prefs.getBoolean("syncLabelsWithAccent", false),
            syncMainTextWithAccent = prefs.getBoolean("syncMainTextWithAccent", false),
            bgRotation = prefs.getFloat("bgRotation", 0f),
            bgOffsetX = prefs.getFloat("bgOffsetX", 0f),
            bgOffsetY = prefs.getFloat("bgOffsetY", 0f),
            bgScale = prefs.getFloat("bgScale", 1f),
            textShadowIntensity = prefs.getFloat("textShadowIntensity", 0f)
        )
    }

    // Debounce para actualización de widgets
    // Evita reconstruir los widgets por cada cambio mínimo en el slider.
    private var widgetUpdateJob: Job? = null
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    private fun notifyWidgetUpdate(delayMs: Long = 500L) {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            if (delayMs > 0L) delay(delayMs)
            try {
                val context = getApplication<Application>().applicationContext
                val intent = android.content.Intent(context, com.example.calendarapp.widget.WidgetDailyUpdater::class.java).apply {
                    action = com.example.calendarapp.widget.WidgetDailyUpdater.ACTION_FORCE_UPDATE
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- FUNCIONES DE ACTUALIZACIÓN ---

    /** Alterna entre modo oscuro (predeterminado) y modo claro. */
    fun toggleLightMode() {
        val isCurrentlyLight = prefs.getBoolean("isLightMode", false)
        val newIsLight = !isCurrentlyLight
        prefs.edit().putBoolean("isLightMode", newIsLight).apply()

        val newBg    = if (newIsLight) Color(0xFFF2F2F2) else Color(0xFF0A0C0B)
        val newText  = if (newIsLight) Color(0xFF1A1A1A) else Color.White
        val newLabel = if (newIsLight) Color(0xFF555555) else Color.Gray
        val newBgArgb    = newBg.toArgb()
        val newTextArgb  = newText.toArgb()
        val newLabelArgb = newLabel.toArgb()

        prefs.edit()
            .putInt("systemBackgroundColor", newBgArgb)
            .putInt("calendarTextColor", newTextArgb)
            .putInt("secondaryTextColor", newLabelArgb)
            .apply()

        _currentTheme.value = _currentTheme.value.copy(
            systemBackgroundColor = newBg,
            calendarTextColor = newText,
            secondaryTextColor = newLabel
        )
        notifyWidgetUpdate()
    }

    fun isLightMode(): Boolean = prefs.getBoolean("isLightMode", false)

    fun saveGlobalBackgroundImage(uriString: String?) {
        val refreshedImages = _currentTheme.value.backgroundImages.toMutableMap()
        _currentTheme.value = _currentTheme.value.copy(
            globalBackgroundImage = uriString,
            backgroundImages = refreshedImages
        )
        if (uriString == null) prefs.edit().remove("globalBackgroundImage").apply()
        else prefs.edit().putString("globalBackgroundImage", uriString).apply()
        notifyWidgetUpdate()
    }

    fun saveBackgroundImage(month: Int, uriString: String?) {
        val newImages = _currentTheme.value.backgroundImages.toMutableMap()
        if (uriString == null) {
            newImages.remove(month)
            prefs.edit().remove("bg_image_month_$month").apply()
        } else {
            newImages[month] = uriString
            prefs.edit().putString("bg_image_month_$month", uriString).apply()
        }
        _currentTheme.value = _currentTheme.value.copy(backgroundImages = newImages)
        notifyWidgetUpdate()
    }

    fun updateAccentColor(color: Color) {
        _currentTheme.value = _currentTheme.value.copy(accentColor = color)
        prefs.edit().putInt("accentColor", color.toArgb()).apply()
        notifyWidgetUpdate()
    }

    fun updateCardOpacity(value: Float) {
        _currentTheme.value = _currentTheme.value.copy(cardOpacity = value)
        prefs.edit().putFloat("cardOpacity", value).apply()
        notifyWidgetUpdate()
    }

    fun updateBackgroundOpacity(value: Float) {
        _currentTheme.value = _currentTheme.value.copy(backgroundOpacity = value)
        prefs.edit().putFloat("backgroundOpacity", value).apply()
        notifyWidgetUpdate()
    }

    fun updateBlurAmount(value: Float) {
        _currentTheme.value = _currentTheme.value.copy(blurAmount = value)
        prefs.edit().putFloat("blurAmount", value).apply()
        notifyWidgetUpdate()
    }

    fun resetVisibilitySettings() {
        _currentTheme.value = _currentTheme.value.copy(backgroundOpacity = 0.3f, cardOpacity = 1.0f, blurAmount = 0f)
        prefs.edit().putFloat("backgroundOpacity", 0.3f).putFloat("cardOpacity", 1.0f).putFloat("blurAmount", 0f).apply()
        notifyWidgetUpdate()
    }

    fun updateCalendarTextColor(color: Color) {
        _currentTheme.value = _currentTheme.value.copy(calendarTextColor = color, isCustomTextActive = true)
        prefs.edit().putInt("calendarTextColor", color.toArgb()).putBoolean("isCustomTextActive", true).apply()
        notifyWidgetUpdate()
    }

    fun updateCalendarTextBrightness(value: Float) {
        _currentTheme.value = _currentTheme.value.copy(calendarTextBrightness = value)
        prefs.edit().putFloat("calendarTextBrightness", value).apply()
        notifyWidgetUpdate()
    }

    fun resetCalendarTextSettings() {
        _currentTheme.value = _currentTheme.value.copy(
            calendarTextColor = Color.White,
            calendarTextBrightness = 1.0f,
            secondaryTextColor = Color.Gray,
            globalTextScale = 1.0f,
            showBackgroundImages = true,
            syncLabelsWithAccent = false,
            syncMainTextWithAccent = false,
            isCustomTextActive = false,
            textShadowIntensity = 0f
        )
        prefs.edit()
            .putInt("calendarTextColor", Color.White.toArgb())
            .putFloat("calendarTextBrightness", 1.0f)
            .putInt("secondaryTextColor", Color.Gray.toArgb())
            .putFloat("globalTextScale", 1.0f)
            .putBoolean("showBackgroundImages", true)
            .putBoolean("syncLabelsWithAccent", false)
            .putBoolean("syncMainTextWithAccent", false)
            .putBoolean("isCustomTextActive", false)
            .putFloat("textShadowIntensity", 0f)
            .apply()
        notifyWidgetUpdate()
    }

    fun updateShowBackgroundImages(value: Boolean) {
        _currentTheme.value = _currentTheme.value.copy(showBackgroundImages = value)
        prefs.edit().putBoolean("showBackgroundImages", value).apply()
        notifyWidgetUpdate()
    }

    fun updateSyncLabelsWithAccent(value: Boolean) {
        _currentTheme.value = _currentTheme.value.copy(syncLabelsWithAccent = value)
        prefs.edit().putBoolean("syncLabelsWithAccent", value).apply()
        notifyWidgetUpdate()
    }

    fun updateSyncMainTextWithAccent(value: Boolean) {
        _currentTheme.value = _currentTheme.value.copy(syncMainTextWithAccent = value)
        prefs.edit().putBoolean("syncMainTextWithAccent", value).apply()
        notifyWidgetUpdate()
    }

    fun updateSecondaryTextColor(color: Color) {
        _currentTheme.value = _currentTheme.value.copy(secondaryTextColor = color)
        prefs.edit().putInt("secondaryTextColor", color.toArgb()).apply()
        notifyWidgetUpdate()
    }

    fun updateGlobalTextScale(value: Float) {
        _currentTheme.value = _currentTheme.value.copy(globalTextScale = value)
        prefs.edit().putFloat("globalTextScale", value).apply()
        notifyWidgetUpdate()
    }

    fun updateMarkerStyle(style: DayMarkerStyle) {
        _currentTheme.value = _currentTheme.value.copy(markerStyle = style)
        prefs.edit().putString("markerStyle", style.name).apply()
        notifyWidgetUpdate()
    }

    fun updateBorderStyle(style: Int) {
        _currentTheme.value = _currentTheme.value.copy(borderStyle = style)
        prefs.edit().putInt("borderStyle", style).apply()
        notifyWidgetUpdate()
    }

    fun updateTypography(style: Int) {
        _currentTheme.value = _currentTheme.value.copy(typographyStyle = style)
        prefs.edit().putInt("typographyStyle", style).apply()
        notifyWidgetUpdate()
    }

    fun updateSystemBackgroundColor(color: Color) {
        _currentTheme.value = _currentTheme.value.copy(systemBackgroundColor = color)
        prefs.edit().putInt("systemBackgroundColor", color.toArgb()).apply()
        notifyWidgetUpdate()
    }

    fun updateWeekendColor(color: Color) {
        _currentTheme.value = _currentTheme.value.copy(weekendColor = color)
        prefs.edit().putInt("weekendColor", color.toArgb()).apply()
        notifyWidgetUpdate()
    }

    fun updateBackgroundTransformation(rotation: Float, offsetX: Float, offsetY: Float, scale: Float) {
        _currentTheme.value = _currentTheme.value.copy(
            bgRotation = rotation,
            bgOffsetX = offsetX,
            bgOffsetY = offsetY,
            bgScale = scale
        )
        prefs.edit()
            .putFloat("bgRotation", rotation)
            .putFloat("bgOffsetX", offsetX)
            .putFloat("bgOffsetY", offsetY)
            .putFloat("bgScale", scale)
            .apply()
        notifyWidgetUpdate()
    }

    fun updateTextShadowIntensity(value: Float) {
        _currentTheme.value = _currentTheme.value.copy(textShadowIntensity = value)
        prefs.edit().putFloat("textShadowIntensity", value).apply()
        notifyWidgetUpdate()
    }

    // ─── COPIA INTERNA DE IMÁGENES ────────────────────────────────────────────
    /**
     * Copia la imagen del URI externo (galería) a la carpeta privada de la app.
     * Devuelve un URI file:// (ej: "file:///data/.../files/backgrounds/bg_global.jpg")
     * compatible con Coil (AsyncImage) y contentResolver.openInputStream().
     *
     * @param uri URI externo (content://...) elegido por el usuario
     * @param fileName Nombre único del archivo destino (ej: "bg_global.jpg", "bg_month_3.jpg")
     * @return URI file:// como String, o null si falla la copia
     */
    suspend fun copyImageToAppStorage(uri: Uri, fileName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val dir = File(context.filesDir, "backgrounds").apply { mkdirs() }
                val dest = File(dir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                // Devolver URI file:// en vez de path crudo — compatible con Coil y contentResolver
                android.net.Uri.fromFile(dest).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    /**
     * Elimina una imagen de la carpeta interna.
     * Acepta tanto URI file:// como path absoluto.
     */
    fun deleteImageFromAppStorage(uriOrPath: String?) {
        if (uriOrPath.isNullOrEmpty()) return
        try {
            val file = if (uriOrPath.startsWith("file://")) {
                File(android.net.Uri.parse(uriOrPath).path ?: return)
            } else {
                File(uriOrPath)
            }
            if (file.exists() && file.parent?.contains("backgrounds") == true) file.delete()
        } catch (e: Exception) { e.printStackTrace() }
    }
}

// --- UTILIDADES ---
fun calculateColor(base: Color, fraction: Float): Color {
    return if (fraction < 0.5f) lerp(Color.Black, base, fraction * 2f)
    else lerp(base, Color.White, (fraction - 0.5f) * 2f)
}

fun lerp(start: Color, stop: Color, fraction: Float): Color = Color(
    red = start.red + (stop.red - start.red) * fraction,
    green = start.green + (stop.green - start.green) * fraction,
    blue = start.blue + (stop.blue - start.blue) * fraction,
    alpha = start.alpha + (stop.alpha - start.alpha) * fraction
)