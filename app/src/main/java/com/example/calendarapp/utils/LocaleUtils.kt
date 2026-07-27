package com.example.calendarapp.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

object LocaleUtils {
    fun updateLocale(context: Context, lang: String): ContextWrapper {
        val localeCode = if (lang.contains("Español", true)) "es" else "en"
        val locale = java.util.Locale.forLanguageTag(localeCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return ContextWrapper(context.createConfigurationContext(config))
    }
}