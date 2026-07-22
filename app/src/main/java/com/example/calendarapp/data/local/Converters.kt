package com.example.calendarapp.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import com.example.calendarapp.data.model.NotificationConfig
import com.example.calendarapp.data.model.NotificationType
import com.example.calendarapp.data.model.RecurrenceType

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(dateString: String): LocalDate = LocalDate.parse(dateString)

    @TypeConverter
    fun fromNotificationConfig(config: NotificationConfig): String =
        "${config.enabled}|${config.type.name}"

    @TypeConverter
    fun toNotificationConfig(data: String): NotificationConfig {
        val parts = data.split("|")
        if (parts.size == 2) {
            return NotificationConfig(
                enabled = parts[0].toBoolean(),
                type = NotificationType.valueOf(parts[1])
            )
        }
        return NotificationConfig()
    }

    @TypeConverter
    fun fromRecurrenceType(recurrence: RecurrenceType): String = recurrence.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType =
        try { RecurrenceType.valueOf(value) } catch (e: Exception) { RecurrenceType.NONE }
}
