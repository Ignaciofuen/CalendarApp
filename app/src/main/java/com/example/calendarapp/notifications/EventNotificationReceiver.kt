package com.example.calendarapp.notifications


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class EventNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.example.calendarapp.SNOOZE_EVENT" -> handleSnooze(context, intent)
            "com.example.calendarapp.DISMISS_EVENT" -> handleDismiss(context, intent)
            else -> handleNotification(context, intent)
        }
    }

    private fun handleNotification(context: Context, intent: Intent) {
        val title = intent.getStringExtra("event_title") ?: "Recordatorio"
        val note = intent.getStringExtra("event_note") ?: ""
        val id = intent.getIntExtra("event_id", System.currentTimeMillis().toInt())

        // Intent para posponer 10 minutos
        val snoozeIntent = Intent("com.example.calendarapp.SNOOZE_EVENT").apply {
            setPackage(context.packageName)
            putExtra("event_title", title)
            putExtra("event_note", note)
            putExtra("event_id", id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, id + 10000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para descartar
        val dismissIntent = Intent("com.example.calendarapp.DISMISS_EVENT").apply {
            setPackage(context.packageName)
            putExtra("event_id", id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, id + 20000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para abrir la app al tocar la notificación (null-safe)
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: Intent(context, com.example.calendarapp.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "EVENT_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(note.ifEmpty { "Tu evento está por comenzar" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Posponer 10 min", snoozePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Descartar", dismissPendingIntent)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val title = intent.getStringExtra("event_title") ?: "Recordatorio"
        val note = intent.getStringExtra("event_note") ?: ""
        val id = intent.getIntExtra("event_id", 0)

        // Cancelar la notificación actual
        NotificationManagerCompat.from(context).cancel(id)

        // Reprogramar en 10 minutos
        val snoozeMillis = System.currentTimeMillis() + 10 * 60 * 1000L
        val newIntent = Intent("com.example.calendarapp.EVENT_NOTIFICATION").apply {
            setPackage(context.packageName)
            putExtra("event_title", title)
            putExtra("event_note", note)
            putExtra("event_id", id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeMillis, pendingIntent)
        }
    }

    private fun handleDismiss(context: Context, intent: Intent) {
        val id = intent.getIntExtra("event_id", 0)
        NotificationManagerCompat.from(context).cancel(id)
    }
}
