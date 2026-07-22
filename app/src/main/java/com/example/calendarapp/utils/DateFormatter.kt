package com.example.calendarapp.utils



import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DateFormatType(val pattern: String, val example: String) {
    DD_MM_YYYY("dd/MM/yyyy", "25/10/2023"),
    MM_DD_YYYY("MM/dd/yyyy", "10/25/2023"),
    YYYY_MM_DD("yyyy-MM-dd", "2023-10-25")
}

object DateFormatter {
    fun format(date: LocalDate, type: DateFormatType): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern(type.pattern, Locale.getDefault())
            date.format(formatter)
        } catch (e: Exception) {
            date.toString()
        }
    }
}