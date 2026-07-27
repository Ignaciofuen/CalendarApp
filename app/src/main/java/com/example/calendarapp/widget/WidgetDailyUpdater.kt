package com.example.calendarapp.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Calendar

/**
 * Receiver responsable de actualizar los widgets al cambiar el día.
 *
 * Estrategia multicapa (de más a menos confiable):
 *   1. AlarmManager exacto a medianoche — se reprograma solo cada día
 *   2. BOOT_COMPLETED — reprograma la alarma después de un reinicio
 *   3. DATE_CHANGED / TIME_SET / TIMEZONE_CHANGED — fallback del sistema
 *   4. updatePeriodMillis="1800000" en los XMLs — fallback de 30 min del sistema
 */
class WidgetDailyUpdater : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MIDNIGHT_UPDATE,
            ACTION_FORCE_UPDATE,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",   // HTC/Huawei boot
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                val pendingResult = goAsync()
                val job = SupervisorJob()
                val scope = CoroutineScope(Dispatchers.Main + job)
                scope.launch {
                    try {
                        val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                        val forceKey = androidx.datastore.preferences.core.longPreferencesKey("force_update_time")
                        
                        supervisorScope {
                            val widgets = listOf(
                                TodayCalendarWidget(),
                                WeekCalendarWidget(),
                                MonthCalendarWidget(),
                                UpcomingEventsWidget()
                            )
                            
                            widgets.map { widget ->
                                async {
                                    val glanceIds = manager.getGlanceIds(widget.javaClass)
                                    glanceIds.forEach { id ->
                                        androidx.glance.appwidget.state.updateAppWidgetState(context, id) { prefs ->
                                            prefs[forceKey] = System.currentTimeMillis()
                                        }
                                        widget.update(context, id)
                                    }
                                }
                            }.forEach { it.await() }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        job.cancel()  // Libera el scope - sin memory leak
                        pendingResult.finish()
                    }
                }
                // Siempre reprogramar la próxima alarma de medianoche
                scheduleNextMidnight(context)
            }
        }
    }

    companion object {
        const val ACTION_MIDNIGHT_UPDATE = "com.example.calendarapp.MIDNIGHT_WIDGET_UPDATE"
        const val ACTION_FORCE_UPDATE = "com.example.calendarapp.FORCE_WIDGET_UPDATE"
        private const val REQUEST_CODE = 9001

        /**
         * Programa una alarma para las 00:00:15 del día siguiente.
         * Usa setExactAndAllowWhileIdle si el permiso está disponible,
         * con fallback a setWindow para máxima compatibilidad.
         */
        fun scheduleNextMidnight(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, WidgetDailyUpdater::class.java).apply {
                action = ACTION_MIDNIGHT_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calcular las 00:00:15 del próximo día para evitar el instante exacto de medianoche
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 15)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        // API 31+ con permiso concedido → alarma exacta que despierta en Doze
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, nextMidnight, pendingIntent
                        )
                    } else {
                        // Sin permiso exacto → ventana de 20 min desde medianoche
                        alarmManager.setWindow(
                            AlarmManager.RTC_WAKEUP,
                            nextMidnight,
                            20 * 60 * 1000L,
                            pendingIntent
                        )
                    }
                } else {
                    // API < 31 → setExactAndAllowWhileIdle sin restricciones
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, nextMidnight, pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Fallback final: ventana de 20 minutos desde medianoche
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight,
                    20 * 60 * 1000L,
                    pendingIntent
                )
            }
        }
    }
}
