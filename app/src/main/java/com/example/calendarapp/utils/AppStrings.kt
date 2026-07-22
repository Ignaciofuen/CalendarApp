package com.example.calendarapp.utils

object AppStrings {
    private val translations = mapOf(
        "en" to mapOf(
            "preferences_title" to "Preferences",
            "app_language" to "App Language",
            "save_changes" to "Save Changes",
            "start_week_on" to "Start Week On",
            "todays_focus" to "Today's Focus",
            "no_events" to "No events today",
            "calendar" to "Calendar",
            "schedule" to "Schedule",
            "settings" to "Settings",
            "new_event" to "New Event",
            "edit_event" to "Edit Event",
            "event_title" to "Event Title",
            "delete" to "Delete",
            "cancel" to "Cancel",
            "save" to "Save",
            "design" to "Design"

        ),
        "es" to mapOf(
            "preferences_title" to "Preferencias",
            "app_language" to "Idioma de App",
            "save_changes" to "Guardar Cambios",
            "start_week_on" to "Semana inicia en",
            "todays_focus" to "Enfoque de Hoy",
            "no_events" to "No hay eventos hoy",
            "calendar" to "Calendario",
            "schedule" to "Agenda",
            "settings" to "Ajustes",
            "new_event" to "Nuevo Evento",
            "edit_event" to "Editar Evento",
            "event_title" to "Título del evento",
            "delete" to "Borrar",
            "cancel" to "Cancelar",
            "save" to "Guardar",
            "design" to "Diseño"
        )
    )

    fun get(key: String, lang: String): String {
        val code = if (lang.contains("Español", ignoreCase = true)) "es" else "en"
        return translations[code]?.get(key) ?: key
    }
}