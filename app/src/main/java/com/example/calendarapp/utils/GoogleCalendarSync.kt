package com.example.calendarapp.utils

import android.content.Context
import android.util.Log
import com.example.calendarapp.data.local.AppDatabase
import com.example.calendarapp.data.model.CalendarEvent
import com.example.calendarapp.data.model.RecurrenceType
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Sincronización bidireccional con Google Calendar usando OAuth 2.0.
 *
 * SETUP REQUERIDO (una sola vez):
 * ─────────────────────────────────────────────────────────────────
 * 1. Ir a https://console.cloud.google.com y crear un proyecto nuevo.
 * 2. Activar "Google Calendar API" en "APIs y servicios".
 * 3. Ir a "Credenciales" → "Crear credenciales" → "ID de cliente de OAuth 2.0".
 * 4. Tipo de aplicación: Android.
 * 5. Nombre del paquete: com.example.calendarapp
 * 6. Huella digital SHA-1: ejecutar en terminal →
 *      keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
 * 7. Descargar el archivo google-services.json y copiarlo a /app/
 * 8. Añadir el plugin en app/build.gradle.kts:
 *      plugins { id("com.google.gms.google-services") }
 * 9. Y en el build.gradle raíz:
 *      plugins { id("com.google.gms.google-services") version "4.4.1" apply false }
 * ─────────────────────────────────────────────────────────────────
 */
object GoogleCalendarSync {

    private const val TAG = "GoogleCalendarSync"
    private const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
    private const val CALENDAR_API   = "https://www.googleapis.com/calendar/v3"

    private val httpClient = OkHttpClient()

    // ─── OPCIONES DE SIGN-IN ─────────────────────────────────────────

    fun buildSignInOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CALENDAR_SCOPE))
            .build()

    /** Devuelve la cuenta de Google actualmente autenticada, o null. */
    fun getSignedInAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    /** Devuelve true si el usuario está autenticado Y tiene el scope de Calendar. */
    fun isSignedInWithCalendar(context: Context): Boolean {
        val account = getSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, Scope(CALENDAR_SCOPE))
    }

    // ─── OBTENER TOKEN ───────────────────────────────────────────────

    /**
     * Obtiene el access token OAuth para el account dado.
     * Debe llamarse en un hilo IO (no en el main thread).
     */
    private suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val account = getSignedInAccount(context) ?: return@withContext null
        try {
            com.google.android.gms.auth.GoogleAuthUtil.getToken(
                context,
                account.account!!,
                "oauth2:$CALENDAR_SCOPE"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo token: ${e.message}")
            null
        }
    }

    // ─── IMPORTAR DESDE GOOGLE CALENDAR ─────────────────────────────

    /**
     * Descarga eventos del calendario "primary" de Google Calendar
     * e inserta en la BD local los que no existan (identificados por gcalId en el campo note).
     * Retorna el número de eventos nuevos importados.
     */
    suspend fun importFromGoogleCalendar(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val token = getAccessToken(context) ?: error("No autenticado con Google")
            val dao = AppDatabase.getDatabase(context).eventDao()
            val existingGcalIds = dao.getAllEventsSync()
                .filter { it.note.startsWith("gcal:") }
                .map { it.note }
                .toSet()

            // Rango: 90 días atrás → 365 días adelante
            val now = java.time.OffsetDateTime.now(ZoneOffset.UTC)
            val timeMin = now.minusDays(90).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val timeMax = now.plusDays(365).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            val url = "$CALENDAR_API/calendars/primary/events" +
                "?singleEvents=true&orderBy=startTime" +
                "&timeMin=${java.net.URLEncoder.encode(timeMin, "UTF-8")}" +
                "&timeMax=${java.net.URLEncoder.encode(timeMax, "UTF-8")}" +
                "&maxResults=250"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) error("Error API: ${response.code}")

            val body = response.body?.string() ?: error("Respuesta vacía")
            val items = JSONObject(body).optJSONArray("items") ?: JSONArray()
            var count = 0

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val gcalId = item.optString("id") ?: continue
                val noteKey = "gcal:$gcalId"
                if (existingGcalIds.contains(noteKey)) continue

                val event = parseGCalEvent(item, noteKey) ?: continue
                dao.insertEvent(event)
                count++
            }
            count
        }
    }

    // ─── CREAR EVENTO EN GOOGLE CALENDAR ────────────────────────────

    /**
     * Crea un evento en el calendario "primary" de Google Calendar.
     * Actualiza el campo note del evento local con el ID de GCal asignado.
     */
    suspend fun createEventInGoogleCalendar(context: Context, event: CalendarEvent): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = getAccessToken(context) ?: error("No autenticado con Google")

                val body = buildGCalEventJson(event).toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$CALENDAR_API/calendars/primary/events")
                    .addHeader("Authorization", "Bearer $token")
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) error("Error creando evento: ${response.code}")

                val responseBody = response.body?.string() ?: error("Respuesta vacía")
                val gcalId = JSONObject(responseBody).getString("id")

                // Actualizamos el evento local con el ID de GCal
                val dao = AppDatabase.getDatabase(context).eventDao()
                dao.updateEvent(event.copy(note = "gcal:$gcalId"))

                gcalId
            }
        }

    // ─── ELIMINAR EVENTO EN GOOGLE CALENDAR ─────────────────────────

    /**
     * Elimina el evento correspondiente en Google Calendar (si fue creado desde la app).
     */
    suspend fun deleteEventFromGoogleCalendar(context: Context, event: CalendarEvent): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!event.note.startsWith("gcal:")) return@runCatching
                val gcalId = event.note.removePrefix("gcal:")
                val token = getAccessToken(context) ?: error("No autenticado con Google")

                val request = Request.Builder()
                    .url("$CALENDAR_API/calendars/primary/events/$gcalId")
                    .addHeader("Authorization", "Bearer $token")
                    .delete()
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful && response.code != 404) {
                    error("Error eliminando evento: ${response.code}")
                }
            }
        }

    // ─── HELPERS DE PARSEO ───────────────────────────────────────────

    private fun parseGCalEvent(item: JSONObject, noteKey: String): CalendarEvent? {
        return try {
            val title   = item.optString("summary").ifBlank { return null }
            val start   = item.optJSONObject("start") ?: return null
            val end     = item.optJSONObject("end")
            val loc     = item.optString("location")
            val rrule   = item.optJSONArray("recurrence")
                ?.let { arr -> (0 until arr.length()).map { arr.getString(it) }
                    .firstOrNull { it.startsWith("RRULE:") }?.removePrefix("RRULE:") } ?: ""

            val allDayStr = start.optString("date")    // solo si es all-day
            val startStr  = start.optString("dateTime") // si tiene hora

            val isAllDay = allDayStr.isNotEmpty()
            val date: LocalDate
            val startTime: String
            val endTime: String

            if (isAllDay) {
                date      = LocalDate.parse(allDayStr)
                startTime = ""
                endTime   = ""
            } else {
                val ldt   = java.time.OffsetDateTime.parse(startStr).toLocalDateTime()
                date      = ldt.toLocalDate()
                startTime = ldt.toLocalTime().toString()
                endTime   = end?.optString("dateTime")?.let {
                    java.time.OffsetDateTime.parse(it).toLocalTime().toString()
                } ?: ""
            }

            val recurrence = when {
                rrule.contains("FREQ=DAILY",   ignoreCase = true) -> RecurrenceType.DAILY
                rrule.contains("FREQ=WEEKLY",  ignoreCase = true) -> RecurrenceType.WEEKLY
                rrule.contains("FREQ=MONTHLY", ignoreCase = true) -> RecurrenceType.MONTHLY
                rrule.contains("FREQ=YEARLY",  ignoreCase = true) -> RecurrenceType.YEARLY
                else -> RecurrenceType.NONE
            }

            CalendarEvent(
                title = title,
                time = startTime,
                date = date,
                note = noteKey,
                endTime = endTime,
                recurrence = recurrence,
                isAllDay = isAllDay,
                location = loc
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando evento GCal: ${e.message}")
            null
        }
    }

    private fun buildGCalEventJson(event: CalendarEvent): JSONObject {
        val obj = JSONObject()
        obj.put("summary", event.title)
        if (event.location.isNotEmpty()) obj.put("location", event.location)

        if (event.isAllDay) {
            val dateStr = event.date.toString()
            obj.put("start", JSONObject().put("date", dateStr))
            obj.put("end",   JSONObject().put("date", event.date.plusDays(1).toString()))
        } else {
            val startIso = event.date.atTime(LocalTime.parse(event.time))
                .atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val endIso = if (event.endTime.isNotEmpty())
                event.date.atTime(LocalTime.parse(event.endTime))
                    .atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            else
                event.date.atTime(LocalTime.parse(event.time)).plusHours(1)
                    .atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            obj.put("start", JSONObject().put("dateTime", startIso).put("timeZone", "UTC"))
            obj.put("end",   JSONObject().put("dateTime", endIso).put("timeZone", "UTC"))
        }

        if (event.recurrence != RecurrenceType.NONE) {
            val freq = when (event.recurrence) {
                RecurrenceType.DAILY   -> "DAILY"
                RecurrenceType.WEEKLY  -> "WEEKLY"
                RecurrenceType.MONTHLY -> "MONTHLY"
                RecurrenceType.YEARLY  -> "YEARLY"
                else -> null
            }
            if (freq != null) obj.put("recurrence", JSONArray().put("RRULE:FREQ=$freq"))
        }

        return obj
    }
}
