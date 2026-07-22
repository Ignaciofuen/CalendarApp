package com.example.calendarapp.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import com.example.calendarapp.utils.DateFormatType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // 1. INICIALIZAMOS EL "BLOC DE NOTAS" (SharedPreferences)
    private val prefs = application.getSharedPreferences("settings_preferences", Context.MODE_PRIVATE)

    // --- ESTADOS REALES (Lo que el sistema usa) ---
    private val _currentFormat = MutableStateFlow(DateFormatType.DD_MM_YYYY)
    val currentFormat: StateFlow<DateFormatType> = _currentFormat.asStateFlow()

    private val _currentLanguage = MutableStateFlow("English")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _startWeekOn = MutableStateFlow("Monday")
    val startWeekOn: StateFlow<String> = _startWeekOn.asStateFlow()

    // --- ESTADOS TEMPORALES (Borradores para la interfaz) ---
    var draftLanguage by mutableStateOf("English")
    var draftFormat by mutableStateOf(DateFormatType.DD_MM_YYYY)

    init {
        // 2. AL ABRIR LA APP, CARGAMOS LA CONFIGURACIÓN GUARDADA
        loadSavedSettings()
    }

    private fun loadSavedSettings() {
        // Cargar Idioma
        val savedLanguage = prefs.getString("language", "English") ?: "English"
        _currentLanguage.value = savedLanguage
        draftLanguage = savedLanguage

        // Cargar Formato de Fecha
        val savedFormatName = prefs.getString("dateFormat", DateFormatType.DD_MM_YYYY.name) ?: DateFormatType.DD_MM_YYYY.name
        val savedFormat = try {
            DateFormatType.valueOf(savedFormatName)
        } catch (e: Exception) {
            DateFormatType.DD_MM_YYYY
        }
        _currentFormat.value = savedFormat
        draftFormat = savedFormat

        // Cargar Inicio de Semana
        val savedStartWeek = prefs.getString("startWeekOn", "Monday") ?: "Monday"
        _startWeekOn.value = savedStartWeek

        // Aplicar el idioma al sistema operativo inmediatamente
        applyLanguageToSystem(savedLanguage)
    }

    fun updateDraftLanguage(language: String) {
        draftLanguage = language
    }

    fun updateDraftFormat(format: DateFormatType) {
        draftFormat = format
    }

    fun updateStartWeek(day: String) {
        _startWeekOn.value = day
        // Guardamos el día de inicio de semana inmediatamente al tocarlo
        prefs.edit().putString("startWeekOn", day).apply()
    }

    /**
     * Aplica los cambios de idioma y formato al sistema global de la aplicación.
     * Y AHORA LOS GUARDA EN LA MEMORIA DEL TELÉFONO.
     */
    fun saveChanges() {
        _currentFormat.value = draftFormat
        _currentLanguage.value = draftLanguage

        // 3. GUARDAR EN SharedPreferences
        prefs.edit()
            .putString("language", draftLanguage)
            .putString("dateFormat", draftFormat.name)
            .apply()

        applyLanguageToSystem(draftLanguage)
    }

    private fun applyLanguageToSystem(language: String) {
        val localeCode = if (language.contains("Español", ignoreCase = true)) "es" else "en"

        // Cambiamos el Locale global para que funciones como getDisplayName funcionen
        val locale = Locale(localeCode)
        Locale.setDefault(locale)

        // Notificamos al sistema operativo para que cambie los recursos XML
        val appLocale = LocaleListCompat.forLanguageTags(localeCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun syncDrafts() {
        draftLanguage = _currentLanguage.value
        draftFormat = _currentFormat.value
    }
}