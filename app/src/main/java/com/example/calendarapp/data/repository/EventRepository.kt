package com.example.calendarapp.data.repository

import com.example.calendarapp.data.local.EventDao
import com.example.calendarapp.data.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    // Obtiene todos los eventos de la base de datos de Room en tiempo real
    val allEvents: Flow<List<CalendarEvent>> = eventDao.getAllEvents()

    // Consulta por rango de fechas
    fun getEventsByDateRange(startDate: String, endDate: String): Flow<List<CalendarEvent>> {
        return eventDao.getEventsByDateRange(startDate, endDate)
    }

    // Solo eventos recurrentes (para expansión sin cargar todos)
    fun getRecurringEvents(): Flow<List<CalendarEvent>> {
        return eventDao.getRecurringEvents()
    }

    // Inserta un evento y devuelve el ID generado automáticamente por la base de datos
    suspend fun insertEvent(event: CalendarEvent): Long {
        return eventDao.insertEvent(event)
    }

    // Actualiza un evento existente
    suspend fun updateEvent(event: CalendarEvent) {
        eventDao.updateEvent(event)
    }

    // Borra un evento basándose en su ID único
    suspend fun deleteEvent(id: Long) {
        eventDao.deleteEvent(id)
    }
}