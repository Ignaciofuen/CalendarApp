package com.example.calendarapp.utils

import android.content.Context
import android.os.Environment
import android.provider.ContactsContract
import com.example.calendarapp.data.local.AppDatabase
import com.example.calendarapp.data.model.CalendarEvent
import com.example.calendarapp.data.model.RecurrenceType
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DataManager {

    // ─── EXPORTAR ICS ───────────────────────────────────────────────
    suspend fun exportToIcs(context: Context): Result<File> = withContext(Dispatchers.IO) { runCatching {
        val events = AppDatabase.getDatabase(context).eventDao().getAllEventsSync()
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//CalendarApp//EN")
        sb.appendLine("CALSCALE:GREGORIAN")

        val dtFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

        for (event in events) {
            val startTime = try { LocalTime.parse(event.time) } catch (e: Exception) { LocalTime.of(12, 0) }
            val startDt = event.date.atTime(startTime)
            val endDt = if (event.endTime.isNotEmpty()) {
                try { event.date.atTime(LocalTime.parse(event.endTime)) } catch (e: Exception) { startDt.plusHours(1) }
            } else startDt.plusHours(1)

            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:${event.id}@calendarapp")
            sb.appendLine("DTSTART:${startDt.format(dtFmt)}")
            sb.appendLine("DTEND:${endDt.format(dtFmt)}")
            sb.appendLine("SUMMARY:${event.title.escapeIcs()}")
            if (event.note.isNotEmpty()) sb.appendLine("DESCRIPTION:${event.note.escapeIcs()}")
            if (event.recurrence.name != "NONE") {
                val rrule = when (event.recurrence.name) {
                    "DAILY" -> "FREQ=DAILY"
                    "WEEKLY" -> "FREQ=WEEKLY"
                    "MONTHLY" -> "FREQ=MONTHLY"
                    "YEARLY" -> "FREQ=YEARLY"
                    else -> null
                }
                if (rrule != null) sb.appendLine("RRULE:$rrule")
            }
            sb.appendLine("END:VEVENT")
        }

        sb.appendLine("END:VCALENDAR")

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(dir, "calendar_export_${System.currentTimeMillis()}.ics")
        file.writeText(sb.toString())
        file
    } }

    // ─── EXPORTAR CSV ───────────────────────────────────────────────
    suspend fun exportToCsv(context: Context): Result<File> = withContext(Dispatchers.IO) { runCatching {
        val events = AppDatabase.getDatabase(context).eventDao().getAllEventsSync()
        val sb = StringBuilder()
        sb.appendLine("ID,Título,Fecha,Hora inicio,Hora fin,Nota,Color,Recurrencia,Notificación")
        for (event in events) {
            sb.appendLine("${event.id},\"${event.title.escapeCsv()}\",${event.date},${event.time},${event.endTime},\"${event.note.escapeCsv()}\",${event.colorHex},${event.recurrence.name},${event.notification.enabled}")
        }
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(dir, "calendar_export_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString())
        file
    } }

    // ─── BACKUP DB ──────────────────────────────────────────────────
    fun backupDatabase(context: Context): Result<File> = runCatching {
        // NOTA: NO cerramos la instancia de Room porque causaría crash en los observers activos.
        // El WAL checkpoint garantiza que todos los datos estén escritos antes de copiar.

        val dbFile = context.getDatabasePath("calendar_database")
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir

        // WAL checkpoint: forzar que todos los datos pendientes se escriban al archivo principal
        try {
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
            )
            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
            db.close()
        } catch (e: Exception) { /* Si falla el checkpoint, copiamos de todas formas */ }

        val backupFile = File(dir, "calendar_backup_${System.currentTimeMillis()}.db")
        dbFile.copyTo(backupFile, overwrite = true)

        // Copiar archivos de journal WAL/SHM si existen
        val walFile = File(dbFile.absolutePath + "-wal")
        val shmFile = File(dbFile.absolutePath + "-shm")
        if (walFile.exists()) walFile.copyTo(File(backupFile.absolutePath + "-wal"), overwrite = true)
        if (shmFile.exists()) shmFile.copyTo(File(backupFile.absolutePath + "-shm"), overwrite = true)

        backupFile
    }

    // ─── RESTORE DB ─────────────────────────────────────────────────
    fun restoreDatabase(context: Context, backupFile: File): Result<Unit> = runCatching {
        AppDatabase.closeInstance()
        val dbFile = context.getDatabasePath("calendar_database")
        backupFile.copyTo(dbFile, overwrite = true)

        // Restaurar archivos WAL/SHM si existen en el backup
        val walBackup = File(backupFile.absolutePath + "-wal")
        val shmBackup = File(backupFile.absolutePath + "-shm")
        val walTarget = File(dbFile.absolutePath + "-wal")
        val shmTarget = File(dbFile.absolutePath + "-shm")
        if (walBackup.exists()) walBackup.copyTo(walTarget, overwrite = true) else walTarget.delete()
        if (shmBackup.exists()) shmBackup.copyTo(shmTarget, overwrite = true) else shmTarget.delete()
    }

    // ─── IMPORTAR ICS ───────────────────────────────────────────────
    /**
     * Parsea un archivo .ics (VCALENDAR) desde un InputStream y lo inserta en la DB.
     * Retorna el número de eventos importados.
     */
    suspend fun importFromIcs(context: Context, inputStream: InputStream): Result<Int> = runCatching {
        val dao = AppDatabase.getDatabase(context).eventDao()
        val lines = inputStream.bufferedReader().readLines()
        var count = 0

        // Unfold: las líneas que empiezan con espacio/tab son continuación de la anterior
        val unfolded = mutableListOf<String>()
        for (line in lines) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && unfolded.isNotEmpty()) {
                unfolded[unfolded.lastIndex] += line.trimStart()
            } else {
                unfolded.add(line)
            }
        }

        var inEvent = false
        var summary = ""; var dtStart = ""; var dtEnd = ""
        var description = ""; var rrule = ""; var location = ""

        for (rawLine in unfolded) {
            val line = rawLine.trim()
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true; summary = ""; dtStart = ""; dtEnd = ""
                    description = ""; rrule = ""; location = ""
                }
                line.equals("END:VEVENT", ignoreCase = true) && inEvent -> {
                    inEvent = false
                    val event = parseIcsEvent(summary, dtStart, dtEnd, description, rrule, location)
                    if (event != null) { dao.insertEvent(event); count++ }
                }
                inEvent -> {
                    val colon = line.indexOf(':')
                    if (colon < 0) continue
                    val key = line.substring(0, colon).uppercase().split(";")[0]
                    val value = line.substring(colon + 1).replace("\\,", ",").replace("\\n", "\n")
                    when (key) {
                        "SUMMARY"     -> summary = value
                        "DTSTART"     -> dtStart = value
                        "DTEND"       -> dtEnd = value
                        "DESCRIPTION" -> description = value
                        "RRULE"       -> rrule = value
                        "LOCATION"    -> location = value
                    }
                }
            }
        }
        count
    }

    private fun parseIcsEvent(
        summary: String, dtStart: String, dtEnd: String,
        description: String, rrule: String, location: String
    ): CalendarEvent? {
        if (summary.isBlank() || dtStart.isBlank()) return null
        return try {
            val isAllDay = dtStart.length == 8 // yyyyMMdd sin hora
            val date: LocalDate
            val startTime: LocalTime
            val endTime: String

            if (isAllDay) {
                date = LocalDate.parse(dtStart, DateTimeFormatter.BASIC_ISO_DATE)
                startTime = LocalTime.MIDNIGHT
                endTime = ""
            } else {
                val dtClean = dtStart.replace("Z", "").take(15)
                val dtFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val ldt = java.time.LocalDateTime.parse(dtClean, dtFmt)
                date = ldt.toLocalDate()
                startTime = ldt.toLocalTime()
                endTime = if (dtEnd.isNotBlank()) {
                    val endClean = dtEnd.replace("Z", "").take(15)
                    try {
                        java.time.LocalDateTime.parse(endClean, dtFmt).toLocalTime().toString()
                    } catch (e: Exception) { "" }
                } else ""
            }

            val recurrence = when {
                rrule.contains("FREQ=DAILY",   ignoreCase = true) -> RecurrenceType.DAILY
                rrule.contains("FREQ=WEEKLY",  ignoreCase = true) -> RecurrenceType.WEEKLY
                rrule.contains("FREQ=MONTHLY", ignoreCase = true) -> RecurrenceType.MONTHLY
                rrule.contains("FREQ=YEARLY",  ignoreCase = true) -> RecurrenceType.YEARLY
                else -> RecurrenceType.NONE
            }

            CalendarEvent(
                title = summary,
                time = if (isAllDay) "" else startTime.toString(),
                date = date,
                note = description,
                endTime = endTime,
                recurrence = recurrence,
                isAllDay = isAllDay,
                location = location
            )
        } catch (e: Exception) { null }
    }

    // ─── IMPORTAR CUMPLEAÑOS DESDE CONTACTOS ────────────────────────
    /**
     * Lee los cumpleaños almacenados en los contactos del dispositivo y crea
     * eventos recurrentes (YEARLY) para el año actual.
     * Requiere permiso READ_CONTACTS.
     * Retorna la lista de eventos insertados.
     */
    suspend fun importBirthdaysFromContacts(context: Context): Result<Int> = runCatching {
        val dao = AppDatabase.getDatabase(context).eventDao()
        val existingEvents = dao.getAllEventsSync()
        val existingBirthdays = existingEvents
            .filter { it.recurrence == RecurrenceType.YEARLY && it.note == "birthday_import" }
            .map { it.title }
            .toSet()

        val contentResolver = context.contentResolver
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Event.START_DATE
        )
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
        )

        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null) ?: return@runCatching 0
        var count = 0
        val currentYear = LocalDate.now().year

        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(0) ?: continue
                val rawDate = it.getString(1) ?: continue
                if (existingBirthdays.contains("🎂 $name")) continue

                // El formato puede ser --MM-DD (sin año) o yyyy-MM-dd
                val date: LocalDate? = try {
                    if (rawDate.startsWith("--")) {
                        val parts = rawDate.removePrefix("--").split("-")
                        var month = parts[0].toInt()
                        var day = parts[1].toInt()
                        // Feb 29 en año no bisiesto → usar Feb 28
                        if (month == 2 && day == 29 && !java.time.Year.isLeap(currentYear.toLong())) day = 28
                        LocalDate.of(currentYear, month, day)
                    } else {
                        val d = LocalDate.parse(rawDate)
                        // Misma protección para el formato yyyy-MM-dd
                        val targetDay = if (d.monthValue == 2 && d.dayOfMonth == 29 && !java.time.Year.isLeap(currentYear.toLong())) 28 else d.dayOfMonth
                        LocalDate.of(currentYear, d.monthValue, targetDay)
                    }
                } catch (e: Exception) { null }

                if (date != null) {
                    dao.insertEvent(CalendarEvent(
                        title = "🎂 $name",
                        time = "",
                        date = date,
                        note = "birthday_import",
                        recurrence = RecurrenceType.YEARLY,
                        isAllDay = true,
                        colorHex = "#F48FB1"
                    ))
                    count++
                }
            }
        }
        count
    }

    // ─── SINCRONIZAR CON GOOGLE CALENDAR ────────────────────────────
    /**
     * Lee los eventos del calendario principal de Google Calendar (via CalendarContract)
     * e inserta en la DB local los que no existan ya.
     * Requiere permiso READ_CALENDAR.
     * Retorna el número de eventos nuevos importados.
     */
    suspend fun syncFromGoogleCalendar(context: Context): Result<Int> = runCatching {
        val dao = AppDatabase.getDatabase(context).eventDao()
        val existingIds = dao.getAllEventsSync().map { it.note }.filter { it.startsWith("gcal:") }.toSet()

        val contentResolver = context.contentResolver

        // Ventana de tiempo: 30 días atrás → 365 días adelante
        val now = System.currentTimeMillis()
        val rangeStart = now - 30L * 24 * 60 * 60 * 1000
        val rangeEnd   = now + 365L * 24 * 60 * 60 * 1000

        val eventsUri = android.provider.CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(rangeStart.toString())
            .appendPath(rangeEnd.toString())
            .build()

        val projection = arrayOf(
            android.provider.CalendarContract.Instances.EVENT_ID,
            android.provider.CalendarContract.Instances.TITLE,
            android.provider.CalendarContract.Instances.BEGIN,
            android.provider.CalendarContract.Instances.END,
            android.provider.CalendarContract.Instances.ALL_DAY,
            android.provider.CalendarContract.Instances.EVENT_LOCATION,
            android.provider.CalendarContract.Instances.DESCRIPTION,
            android.provider.CalendarContract.Instances.RRULE
        )

        val cursor = contentResolver.query(eventsUri, projection, null, null, null) ?: return@runCatching 0
        var count = 0

        cursor.use {
            while (it.moveToNext()) {
                val gcalId = it.getLong(0)
                val noteKey = "gcal:$gcalId"
                if (existingIds.contains(noteKey)) continue

                val title = it.getString(1) ?: continue
                val beginMs = it.getLong(2)
                val endMs   = it.getLong(3)
                val allDay  = it.getInt(4) == 1
                val eventLocation = it.getString(5) ?: ""
                val description = it.getString(6) ?: ""
                val rrule = it.getString(7) ?: ""

                val beginCal = java.util.Calendar.getInstance().apply { timeInMillis = beginMs }
                val endCal   = java.util.Calendar.getInstance().apply { timeInMillis = endMs }

                val date = LocalDate.of(
                    beginCal.get(java.util.Calendar.YEAR),
                    beginCal.get(java.util.Calendar.MONTH) + 1,
                    beginCal.get(java.util.Calendar.DAY_OF_MONTH)
                )
                val startTime = if (allDay) "" else LocalTime.of(
                    beginCal.get(java.util.Calendar.HOUR_OF_DAY),
                    beginCal.get(java.util.Calendar.MINUTE)
                ).toString()
                val endTime = if (allDay) "" else LocalTime.of(
                    endCal.get(java.util.Calendar.HOUR_OF_DAY),
                    endCal.get(java.util.Calendar.MINUTE)
                ).toString()

                val recurrence = when {
                    rrule.contains("FREQ=DAILY",   ignoreCase = true) -> RecurrenceType.DAILY
                    rrule.contains("FREQ=WEEKLY",  ignoreCase = true) -> RecurrenceType.WEEKLY
                    rrule.contains("FREQ=MONTHLY", ignoreCase = true) -> RecurrenceType.MONTHLY
                    rrule.contains("FREQ=YEARLY",  ignoreCase = true) -> RecurrenceType.YEARLY
                    else -> RecurrenceType.NONE
                }

                dao.insertEvent(CalendarEvent(
                    title = title,
                    time = startTime,
                    date = date,
                    note = noteKey,           // guardamos el ID de GCal para no re-importar
                    endTime = endTime,
                    recurrence = recurrence,
                    isAllDay = allDay,
                    location = eventLocation
                ))
                count++
            }
        }
        count
    }

    private fun String.escapeIcs() = replace(",", "\\,").replace("\n", "\\n")
    private fun String.escapeCsv() = replace("\"", "\"\"")
}
