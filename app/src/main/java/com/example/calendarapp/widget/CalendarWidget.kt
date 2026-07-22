@file:SuppressLint("RestrictedApi")
package com.example.calendarapp.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import com.example.calendarapp.widget.WidgetDailyUpdater
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.calendarapp.MainActivity
import com.example.calendarapp.data.local.AppDatabase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// 1. MODELO DE DATOS
data class WidgetUpcomingEvent(val title: String, val date: LocalDate, val time: String)

data class WidgetEventData(
    val eventsCountByDay: Map<Int, Int>,
    val eventDates: Set<LocalDate>,
    val todayNextEventTitle: String,
    val todayNextEventTime: String,
    val upcomingEvents: List<WidgetUpcomingEvent> = emptyList()
)

// 2. DEFINICIÓN DE WIDGETS

class TodayCalendarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lang = getSavedLanguage(context)
        val isSpanish = lang.contains("Español", ignoreCase = true)
        val eventData = obtenerDatosDeEventos(context, isSpanish)
        val backgroundUri = getSavedUri(context)
        val accentColor = getSavedAccentColor(context)
        val textColor = getSavedTextColor(context)
        val cardOpacity = getSavedCardOpacity(context)
        val showBg = getSavedShowBackgroundImages(context)
        val locale = if (isSpanish) Locale("es", "ES") else Locale.ENGLISH

        provideContent {
            val backgroundBitmap = if (showBg) loadCorrectedBitmap(context, backgroundUri) else null
            val mainIntent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    putExtra("navigate_to_date", LocalDate.now().toString())
}

            Box(modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent))) {
                WidgetBackground(backgroundBitmap)
                val today = LocalDate.now()
                Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(today.month.getDisplayName(JavaTextStyle.SHORT, locale).uppercase(), style = TextStyle(color = ColorProvider(accentColor), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Text(today.dayOfMonth.toString(), style = TextStyle(color = ColorProvider(textColor), fontSize = 64.sp, fontWeight = FontWeight.Bold))
                    Spacer(modifier = GlanceModifier.height(16.dp))
                    EventCard(accentColor, eventData.todayNextEventTitle, eventData.todayNextEventTime, cardOpacity, isSpanish, textColor)
                }
            }
        }
    }
}

class WeekCalendarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lang = getSavedLanguage(context)
        val isSpanish = lang.contains("Español", ignoreCase = true)
        val startWeekOn = getSavedStartWeekOn(context)
        val eventData = obtenerDatosDeEventos(context, isSpanish)
        val backgroundUri = getSavedUri(context)
        val accentColor = getSavedAccentColor(context)
        val textColor = getSavedTextColor(context)
        val cardOpacity = getSavedCardOpacity(context)
        val showBg = getSavedShowBackgroundImages(context)
        val locale = if (isSpanish) Locale("es", "ES") else Locale.ENGLISH

        provideContent {
            val backgroundBitmap = if (showBg) loadCorrectedBitmap(context, backgroundUri) else null
            val mainIntent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    putExtra("navigate_to_date", LocalDate.now().toString())
}

            Box(modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent))) {
                WidgetBackground(backgroundBitmap)
                val today = LocalDate.now()
                // Respetamos la preferencia del usuario (Lunes o Domingo como inicio de semana)
                val startOfWeek = if (startWeekOn == "Monday") {
                    today.minusDays(today.dayOfWeek.value.toLong() - 1)
                } else {
                    // Domingo=7%7=0, Lunes=1%7=1, ..., Sábado=6%7=6
                    today.minusDays((today.dayOfWeek.value % 7).toLong())
                }
                Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                    Text("${today.month.getDisplayName(JavaTextStyle.FULL, locale).uppercase()} ${today.year}", style = TextStyle(color = ColorProvider(accentColor), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        for (i in 0..6) {
                            val currentDate = startOfWeek.plusDays(i.toLong())
                            val isToday = currentDate == today
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.defaultWeight()) {
                                Text(currentDate.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, locale), style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 10.sp))
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                val dayModifier = if (isToday) GlanceModifier.background(accentColor).padding(2.dp).cornerRadius(6.dp) else GlanceModifier.padding(2.dp)
                                Text(text = currentDate.dayOfMonth.toString(), style = TextStyle(color = ColorProvider(if (isToday) Color.Black else textColor), fontSize = 12.sp, fontWeight = FontWeight.Bold), modifier = dayModifier)
                                // Usamos eventDates (set de fechas completas) para detectar eventos cross-month
                                if (currentDate in eventData.eventDates) {
                                    Spacer(modifier = GlanceModifier.size(4.dp).background(if(isToday) Color.Black else accentColor).cornerRadius(2.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    EventCard(accentColor, eventData.todayNextEventTitle, eventData.todayNextEventTime, cardOpacity, isSpanish, textColor)
                }
            }
        }
    }
}

class MonthCalendarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lang = getSavedLanguage(context)
        val isSpanish = lang.contains("Español", ignoreCase = true)
        val startWeekOn = getSavedStartWeekOn(context)
        val eventData = obtenerDatosDeEventos(context, isSpanish)
        val backgroundUri = getSavedUri(context)
        val accentColor = getSavedAccentColor(context)
        val textColor = getSavedTextColor(context)
        val cardOpacity = getSavedCardOpacity(context)
        val showBg = getSavedShowBackgroundImages(context)
        val locale = if (isSpanish) Locale("es", "ES") else Locale.ENGLISH

        provideContent {
            val backgroundBitmap = if (showBg) loadCorrectedBitmap(context, backgroundUri) else null
            val mainIntent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    putExtra("navigate_to_date", LocalDate.now().toString())
}

            Box(modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent))) {
                WidgetBackground(backgroundBitmap)
                val today = LocalDate.now()
                Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
                    Text(today.month.getDisplayName(JavaTextStyle.FULL, locale).uppercase(), style = TextStyle(color = ColorProvider(textColor), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    // Encabezado dinámico: respeta idioma y preferencia de inicio de semana
                    // DayOfWeek: Lun=1 … Dom=7. Si inicia en Lunes, firstDayValue=1; si en Domingo, firstDayValue=7
                    val firstDayValue = if (startWeekOn == "Monday") 1 else 7
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        for (offset in 0..6) {
                            val dayValue = ((firstDayValue - 1 + offset) % 7) + 1
                            val name = java.time.DayOfWeek.of(dayValue)
                                .getDisplayName(JavaTextStyle.NARROW, locale).uppercase()
                            Text(text = name, modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = ColorProvider(accentColor), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                        }
                    }
                    // Calculamos el offset real del primer día del mes según inicio de semana
                    val primerDia = today.withDayOfMonth(1)
                    // Lunes como inicio: Lun=0 … Dom=6  →  offset = dayOfWeek.value - 1
                    // Domingo como inicio: Dom=0, Lun=1 … Sáb=6  →  offset = dayOfWeek.value % 7
                    val startOffset = if (startWeekOn == "Monday")
                        primerDia.dayOfWeek.value - 1
                    else
                        primerDia.dayOfWeek.value % 7
                    val diasEnMes = today.lengthOfMonth()
                    val totalCeldas = startOffset + diasEnMes
                    val filas = (totalCeldas + 6) / 7 // ceil para 5 o 6 filas según el mes

                    for (fila in 0 until filas) {
                        Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp)) {
                            for (col in 0..6) {
                                val cellIndex = fila * 7 + col
                                val dia = cellIndex - startOffset + 1
                                if (cellIndex < startOffset || dia > diasEnMes) {
                                    // Celda vacía antes del primer día o tras el último
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                } else {
                                    val esHoy = dia == today.dayOfMonth
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.defaultWeight()) {
                                        val dayModifier = if (esHoy) GlanceModifier.background(accentColor).padding(1.dp).cornerRadius(4.dp) else GlanceModifier.padding(1.dp)
                                        Text(text = dia.toString(), style = TextStyle(color = ColorProvider(if(esHoy) Color.Black else textColor), fontSize = 10.sp), modifier = dayModifier)
                                        val numEvents = eventData.eventsCountByDay[dia] ?: 0
                                        if (numEvents > 0) Spacer(modifier = GlanceModifier.size(2.dp).background(if(esHoy) Color.Black else accentColor).cornerRadius(1.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    EventCard(accentColor, eventData.todayNextEventTitle, eventData.todayNextEventTime, cardOpacity, isSpanish, textColor)
                }
            }
        }
    }
}

class UpcomingEventsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lang = getSavedLanguage(context)
        val isSpanish = lang.contains("Español", ignoreCase = true)
        val eventData = obtenerDatosDeEventos(context, isSpanish)
        val accentColor = getSavedAccentColor(context)
        val textColor = getSavedTextColor(context)
        val cardOpacity = getSavedCardOpacity(context)
        val locale = if (isSpanish) Locale("es", "ES") else Locale.ENGLISH

        provideContent {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to_date", LocalDate.now().toString())
            }
            val bgColor = Color(0xEE141A17)
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(12.dp)
                    .clickable(actionStartActivity(mainIntent))
            ) {
                Text(
                    if (isSpanish) "Próximos eventos" else "Upcoming events",
                    style = TextStyle(color = ColorProvider(accentColor), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                if (eventData.upcomingEvents.isEmpty()) {
                    Text(
                        if (isSpanish) "Sin eventos próximos" else "No upcoming events",
                        style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp)
                    )
                } else {
                    eventData.upcomingEvents.forEach { ev ->
                        val dateLabel = "${ev.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, locale)} ${ev.date.dayOfMonth} · ${ev.time}"
                        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Spacer(modifier = GlanceModifier.width(3.dp).height(36.dp).background(accentColor).cornerRadius(2.dp))
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Column {
                                Text(ev.title, style = TextStyle(color = ColorProvider(textColor), fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                                Text(dateLabel, style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 10.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. RECEIVERS — GlanceAppWidgetReceiver ya maneja la actualización en super.onReceive()
// Solo agregamos lógica adicional cuando es necesario (ej: programar alarma de medianoche)

class TodayCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayCalendarWidget()
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Programar alarma a medianoche cuando se añade el widget
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            WidgetDailyUpdater.scheduleNextMidnight(context)
        }
    }
}

class WeekCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekCalendarWidget()
}

class MonthCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthCalendarWidget()
}

class UpcomingEventsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingEventsWidget()
}


// 4. LÓGICA COMPARTIDA Y DATOS

@Composable fun EventCard(accentColor: Color, title: String, time: String, opacity: Float, isSpanish: Boolean = false, textColor: Color = Color.White) {
    val backgroundColor = Color(0xFF1C2420).copy(alpha = opacity)
    Column(modifier = GlanceModifier.fillMaxWidth().background(backgroundColor).padding(8.dp).cornerRadius(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = GlanceModifier.width(3.dp).height(10.dp).background(accentColor).cornerRadius(1.5.dp))
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(if (isSpanish) "HOY" else "TODAY", style = TextStyle(color = ColorProvider(accentColor), fontSize = 9.sp, fontWeight = FontWeight.Bold))
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(time, style = TextStyle(color = ColorProvider(textColor.copy(alpha = 0.7f)), fontSize = 9.sp))
        }
        Text(title, style = TextStyle(color = ColorProvider(textColor), fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable fun WidgetBackground(backgroundBitmap: Bitmap?) {
    if (backgroundBitmap != null) {
        Image(provider = ImageProvider(backgroundBitmap), contentDescription = "Widget background", contentScale = androidx.glance.layout.ContentScale.Crop, modifier = GlanceModifier.fillMaxSize())
        Spacer(modifier = GlanceModifier.fillMaxSize().background(Color(0x77000000)))
    } else { Spacer(modifier = GlanceModifier.fillMaxSize().background(Color(0xEE141A17))) }
}

suspend fun obtenerDatosDeEventos(context: Context, isSpanish: Boolean = false): WidgetEventData {
    val noEventsText = if (isSpanish) "Sin eventos hoy" else "No events today"
    return try {
        val database = AppDatabase.getDatabase(context)
        val dao = database.eventDao()
        val today = LocalDate.now()

        // Fase 3: Solo cargamos lo necesario en vez de toda la DB
        val monthStart = today.withDayOfMonth(1).toString()
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth()).toString()

        // Eventos del mes actual (para indicadores en el calendario del widget)
        val monthEvents = dao.getEventsByDateRangeSync(monthStart, monthEnd)

        // Próximos 5 eventos desde hoy (para el widget de upcoming)
        val futureEnd = today.plusMonths(3).toString() // Ventana de 3 meses
        val futureEvents = dao.getEventsByDateRangeSync(today.toString(), futureEnd)

        val eventsCountMap = mutableMapOf<Int, Int>()
        val eventDatesSet = mutableSetOf<LocalDate>()
        var todayTitle = noEventsText; var todayTime = "--:--"; var found = false

        for (event in monthEvents) {
            eventDatesSet.add(event.date)
            eventsCountMap[event.date.dayOfMonth] = eventsCountMap.getOrDefault(event.date.dayOfMonth, 0) + 1
            if (!found && event.date == today) { todayTitle = event.title; todayTime = event.time; found = true }
        }

        // También agregar fechas futuras al set (para cross-month en widget semanal)
        for (event in futureEvents) { eventDatesSet.add(event.date) }

        val upcoming = futureEvents
            .sortedWith(compareBy({ it.date }, { it.time }))
            .take(5)
            .map { WidgetUpcomingEvent(it.title, it.date, it.time) }

        WidgetEventData(eventsCountMap, eventDatesSet, todayTitle, todayTime, upcoming)
    } catch (e: Exception) { WidgetEventData(emptyMap(), emptySet(), noEventsText, "--:--") }
}

fun getSavedLanguage(context: Context): String =
    context.getSharedPreferences("settings_preferences", Context.MODE_PRIVATE)
        .getString("language", "English") ?: "English"

fun getSavedStartWeekOn(context: Context): String =
    context.getSharedPreferences("settings_preferences", Context.MODE_PRIVATE)
        .getString("startWeekOn", "Monday") ?: "Monday"

fun getSavedUri(context: Context): String? {
    val prefs = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)
    val month = LocalDate.now().monthValue
    return prefs.getString("bg_image_month_$month", null) ?: prefs.getString("globalBackgroundImage", null)
}

fun getSavedAccentColor(context: Context): Color = Color(context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE).getInt("accentColor", android.graphics.Color.parseColor("#00E676")))
fun getSavedCardOpacity(context: Context): Float = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE).getFloat("cardOpacity", 1.0f)

fun getSavedShowBackgroundImages(context: Context): Boolean = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE).getBoolean("showBackgroundImages", true)

fun getSavedTextColor(context: Context): Color {
    val prefs = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)
    // Si el usuario activó "Vincular Texto Principal con Acento", devolvemos el acento
    if (prefs.getBoolean("syncMainTextWithAccent", false)) {
        return getSavedAccentColor(context)
    }
    val colorInt = prefs.getInt("calendarTextColor", -1)
    val brightness = prefs.getFloat("calendarTextBrightness", 1.0f)
    val base = Color(if(colorInt == -1) 0xFFFFFFFF.toInt() else colorInt)
    return base.copy(
        red   = (base.red   * brightness).coerceIn(0f, 1f),
        green = (base.green * brightness).coerceIn(0f, 1f),
        blue  = (base.blue  * brightness).coerceIn(0f, 1f)
    )
}

// =========================================================================
// SOLUCIÓN DEFINITIVA: ANTI-CRASH + ROTACIÓN EXIF CORRECTA
// =========================================================================
fun loadCorrectedBitmap(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrEmpty()) return null
    return try {
        val uri = Uri.parse(uriString)

        // 1. LEER LA ROTACIÓN ORIGINAL DE LA CÁMARA (EXIF)
        var rotation = 0f
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = androidx.exifinterface.media.ExifInterface(stream)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            rotation = when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }

        // 2. OBTENER TAMAÑO SIN CARGAR EN MEMORIA (Anti-Crash)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // 3. ESCALAR PARA NO AGOTAR LA RAM
        var inSampleSize = 1
        val reqWidth = 600
        val reqHeight = 600
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        // 4. CARGAR LA IMAGEN REDUCIDA
        val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val compressedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, finalOptions)
        } ?: return null

        // 5. APLICAR ROTACIÓN Y TAMAÑO FINAL CORRECTO
        val matrix = Matrix()
        if (rotation != 0f) {
            matrix.postRotate(rotation)
        }

        val ratio = java.lang.Math.min(800f / compressedBitmap.width, 800f / compressedBitmap.height)
        if (ratio < 1f) {
            matrix.postScale(ratio, ratio)
        }

        // Generamos el bitmap final derecho y optimizado
        Bitmap.createBitmap(
            compressedBitmap,
            0, 0,
            compressedBitmap.width,
            compressedBitmap.height,
            matrix,
            true
        )

    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}