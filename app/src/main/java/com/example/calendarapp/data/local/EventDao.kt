package com.example.calendarapp.data.local

import androidx.room.*
import com.example.calendarapp.data.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    // Para la app principal (En tiempo real) — todos los eventos
    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    // Consulta por rango de fechas para mejorar rendimiento
    @Query("SELECT * FROM events WHERE date >= :startDate AND date <= :endDate")
    fun getEventsByDateRange(startDate: String, endDate: String): Flow<List<CalendarEvent>>

    // Solo eventos recurrentes (para expansión virtual sin cargar todos)
    @Query("SELECT * FROM events WHERE recurrence != 'NONE'")
    fun getRecurringEvents(): Flow<List<CalendarEvent>>

    // Versión suspend para widgets (sin Flow)
    @Query("SELECT * FROM events WHERE recurrence != 'NONE'")
    suspend fun getRecurringEventsSync(): List<CalendarEvent>

    // ---> Consultas directas para widgets (suspend, no Flow) <---
    @Query("SELECT * FROM events")
    suspend fun getAllEventsSync(): List<CalendarEvent>

    // Consulta directa por rango de fechas para widgets
    @Query("SELECT * FROM events WHERE date >= :startDate AND date <= :endDate")
    suspend fun getEventsByDateRangeSync(startDate: String, endDate: String): List<CalendarEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent): Long

    @Update
    suspend fun updateEvent(event: CalendarEvent)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Long)
}