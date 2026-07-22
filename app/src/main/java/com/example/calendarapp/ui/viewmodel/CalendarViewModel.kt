package com.example.calendarapp.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.local.AppDatabase
import com.example.calendarapp.data.model.CalendarEvent
import com.example.calendarapp.data.model.NotificationConfig
import com.example.calendarapp.data.model.NotificationType
import com.example.calendarapp.data.model.RecurrenceType
import com.example.calendarapp.data.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import androidx.glance.appwidget.updateAll
import com.example.calendarapp.widget.TodayCalendarWidget
import com.example.calendarapp.widget.WeekCalendarWidget
import com.example.calendarapp.widget.MonthCalendarWidget
import com.example.calendarapp.widget.UpcomingEventsWidget

// NOTA: Cambiamos 'ViewModel()' por 'AndroidViewModel(application)' para poder usar la Base de Datos
class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventRepository

    // Estado de la fecha seleccionada en el calendario
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Empezamos con un mapa vacío. Room lo llenará automáticamente.
    private val _events = MutableStateFlow<Map<LocalDate, List<CalendarEvent>>>(emptyMap())
    val events: StateFlow<Map<LocalDate, List<CalendarEvent>>> = _events.asStateFlow()

    // Eventos originales de la DB (sin expansión) — usados para editar/buscar por id
    private val _rawEvents = MutableStateFlow<Map<LocalDate, List<CalendarEvent>>>(emptyMap())

    init {
        val eventDao = AppDatabase.getDatabase(application).eventDao()
        repository = EventRepository(eventDao)

        viewModelScope.launch {
            repository.allEvents.collect { eventList ->
                // Guardamos los originales para edición/búsqueda por id
                _rawEvents.value = eventList.groupBy { it.date }
                // Expandimos recurrentes para la UI (±2 años)
                _events.value = expandRecurringEvents(eventList).groupBy { it.date }
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * Añade un nuevo evento a la base de datos y programa la alerta.
     */
    fun addEvent(
        context: Context,
        date: LocalDate,
        title: String,
        time: LocalTime,
        notificationConfig: NotificationConfig = NotificationConfig(),
        colorHex: String = "",
        endTime: String = "",
        recurrence: RecurrenceType = RecurrenceType.NONE,
        isAllDay: Boolean = false,
        location: String = "",
        url: String = "",
        photoUri: String = "",
        notificationSoundUri: String = ""
    ) {
        val newEvent = CalendarEvent(
            title = title,
            time = if (isAllDay) "" else time.toString(),
            date = date,
            notification = notificationConfig,
            colorHex = colorHex,
            endTime = if (isAllDay) "" else endTime,
            recurrence = recurrence,
            isAllDay = isAllDay,
            location = location,
            url = url,
            photoUri = photoUri,
            notificationSoundUri = notificationSoundUri
        )

        viewModelScope.launch {
            val generatedId = repository.insertEvent(newEvent)
            val eventWithId = newEvent.copy(id = generatedId)
            if (eventWithId.notification.enabled && !isAllDay) {
                scheduleNotification(context, eventWithId)
            }
            notifyWidgetUpdate()
        }
    }

    /**
     * Actualiza un evento existente en la base de datos y sincroniza la alarma.
     */
    fun updateEvent(
        context: Context,
        id: Long,
        newDate: LocalDate,          // ← Crítico 2: ahora se puede cambiar la fecha
        newTitle: String,
        newTime: LocalTime,
        newConfig: NotificationConfig,
        colorHex: String = "",
        endTime: String = "",
        recurrence: RecurrenceType = RecurrenceType.NONE,
        isAllDay: Boolean = false,
        location: String = "",
        url: String = "",
        photoUri: String = "",
        notificationSoundUri: String = ""
    ) {
        // Usamos _rawEvents para encontrar el evento original (no instancias virtuales)
        val currentEvent = _rawEvents.value.values.flatten().find { it.id == id } ?: return

        val updatedEvent = currentEvent.copy(
            date = newDate,
            title = newTitle,
            time = if (isAllDay) "" else newTime.toString(),
            notification = newConfig,
            colorHex = colorHex,
            endTime = if (isAllDay) "" else endTime,
            recurrence = recurrence,
            isAllDay = isAllDay,
            location = location,
            url = url,
            photoUri = photoUri,
            notificationSoundUri = notificationSoundUri
        )

        viewModelScope.launch {
            repository.updateEvent(updatedEvent)
            if (updatedEvent.notification.enabled && !isAllDay) {
                scheduleNotification(context, updatedEvent)
            } else {
                cancelNotification(context, updatedEvent)
            }
            notifyWidgetUpdate()
        }
    }

    /** Duplica un evento existente (nuevo ID, mismos datos). */
    fun duplicateEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.insertEvent(event.copy(id = 0))
            notifyWidgetUpdate()
        }
    }

    /**
     * Detecta si existe solapamiento de horario con otros eventos en la misma fecha.
     * Retorna el título del primer evento en conflicto, o null si no hay conflicto.
     */
    fun findConflict(
        date: LocalDate,
        startTime: LocalTime,
        endTimeStr: String,
        isAllDay: Boolean,
        excludeId: Long = -1L
    ): String? {
        if (isAllDay) return null
        // Usamos _events (expandidos) para detectar conflictos con recurrentes también
        val eventsOnDate = _events.value[date] ?: return null
        val newEnd = if (endTimeStr.isNotEmpty())
            try { LocalTime.parse(endTimeStr) } catch (e: Exception) { startTime.plusHours(1) }
        else startTime.plusHours(1)

        return eventsOnDate
            .filter { it.id != excludeId && !it.isAllDay && it.time.isNotEmpty() }
            .firstOrNull { existing ->
                try {
                    val existStart = LocalTime.parse(existing.time)
                    val existEnd = if (existing.endTime.isNotEmpty())
                        LocalTime.parse(existing.endTime)
                    else existStart.plusHours(1)
                    startTime < existEnd && newEnd > existStart
                } catch (e: Exception) { false }
            }?.title
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            // Borramos de Room
            repository.deleteEvent(id)

            // Cancelamos cualquier alarma pendiente de ese evento
            val context = getApplication<Application>().applicationContext
            val dummyEvent = CalendarEvent(id = id, title = "", time = "12:00", date = LocalDate.now())
            cancelNotification(context, dummyEvent)

            notifyWidgetUpdate()
        }
    }

    /**
     * Re-inserta un evento previamente eliminado (acción "Deshacer").
     */
    fun restoreEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.insertEvent(event)
            notifyWidgetUpdate()
        }
    }

    fun updateEventNote(event: CalendarEvent, newNote: String) {
        val updatedEvent = event.copy(note = newNote)
        viewModelScope.launch {
            repository.updateEvent(updatedEvent)
        }
    }

    // --- NOTIFICACIÓN DE WIDGETS ---
    private fun notifyWidgetUpdate() {
        val context = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                TodayCalendarWidget().updateAll(context)
                WeekCalendarWidget().updateAll(context)
                MonthCalendarWidget().updateAll(context)
                UpcomingEventsWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- LÓGICA DE ALARMAS Y PERMISOS (INTACTA) ---

    private fun scheduleNotification(context: Context, event: CalendarEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent("com.example.calendarapp.EVENT_NOTIFICATION").apply {
            setPackage(context.packageName)
            putExtra("event_title", event.title)
            putExtra("event_note", event.note)
            putExtra("event_id", event.id.toInt())
            putExtra("notification_sound_uri", event.notificationSoundUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val eventTime = LocalTime.parse(event.time)
        val calendar = Calendar.getInstance().apply {
            set(event.date.year, event.date.monthValue - 1, event.date.dayOfMonth, eventTime.hour, eventTime.minute)
            set(Calendar.SECOND, 0)
        }

        when (event.notification.type) {
            NotificationType.ONE_DAY_BEFORE -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            NotificationType.TWO_DAYS_BEFORE -> calendar.add(Calendar.DAY_OF_YEAR, -2)
            NotificationType.ONE_WEEK_BEFORE -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            else -> {}
        }

        if (calendar.timeInMillis > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }
    }

    private fun cancelNotification(context: Context, event: CalendarEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.example.calendarapp.EVENT_NOTIFICATION").apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    // ─── Crítico 1: Expansión de eventos recurrentes ───────────────────────────
    /**
     * Genera instancias virtuales de eventos recurrentes dentro del rango dado.
     * Las instancias mantienen el mismo `id` que el original (necesario para editar/eliminar el source).
     * IMPORTANTE: El UI debe usar un key compuesto ("${id}_${date}") en LazyColumn, no solo el `id`.
     * NO se guardan en Room — solo se usan para la UI.
     */
    private fun expandRecurringEvents(
        events: List<CalendarEvent>,
        rangeStart: LocalDate = LocalDate.now().minusYears(1),
        rangeEnd: LocalDate = LocalDate.now().plusYears(2)
    ): List<CalendarEvent> {
        val result = mutableListOf<CalendarEvent>()
        for (event in events) {
            result.add(event) // Siempre incluir el original
            if (event.recurrence == RecurrenceType.NONE) continue

            // Calculamos desde la fecha ORIGINAL para evitar drift (ej: Feb 29 → Feb 28)
            fun nthOccurrence(n: Long): LocalDate = when (event.recurrence) {
                RecurrenceType.DAILY   -> event.date.plusDays(n)
                RecurrenceType.WEEKLY  -> event.date.plusWeeks(n)
                RecurrenceType.MONTHLY -> event.date.plusMonths(n)
                RecurrenceType.YEARLY  -> event.date.plusYears(n)
                RecurrenceType.NONE    -> event.date
            }

            // Ocurrencias hacia adelante (n = 1, 2, 3, ...)
            var n = 1L
            while (true) {
                val d = nthOccurrence(n)
                if (d.isAfter(rangeEnd)) break
                if (!d.isBefore(rangeStart)) result.add(event.copy(date = d))
                n++
            }

            // Ocurrencias hacia atrás (n = -1, -2, -3, ...)
            n = -1L
            while (true) {
                val d = nthOccurrence(n)
                if (d.isBefore(rangeStart)) break
                if (!d.isAfter(rangeEnd)) result.add(event.copy(date = d))
                n--
            }
        }
        return result
    }
}