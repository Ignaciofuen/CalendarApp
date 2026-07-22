package com.example.calendarapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class NotificationType {
    SAME_DAY, ONE_DAY_BEFORE, TWO_DAYS_BEFORE, ONE_WEEK_BEFORE
}

data class NotificationConfig(
    val enabled: Boolean = false,
    val type: NotificationType = NotificationType.SAME_DAY
)

enum class RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

@Entity(tableName = "events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val time: String,                               // "HH:mm" — vacío si isAllDay
    val date: LocalDate,
    val note: String = "",
    val notification: NotificationConfig = NotificationConfig(),
    @ColumnInfo(defaultValue = "") val colorHex: String = "",
    @ColumnInfo(defaultValue = "") val endTime: String = "",
    @ColumnInfo(defaultValue = "NONE") val recurrence: RecurrenceType = RecurrenceType.NONE,
    // ── Campos nuevos (migración 3→4) ──────────────────────────────
    @ColumnInfo(defaultValue = "0")  val isAllDay: Boolean = false,
    @ColumnInfo(defaultValue = "") val location: String = "",
    @ColumnInfo(defaultValue = "") val url: String = "",
    @ColumnInfo(defaultValue = "") val photoUri: String = "",
    @ColumnInfo(defaultValue = "") val notificationSoundUri: String = ""
)
