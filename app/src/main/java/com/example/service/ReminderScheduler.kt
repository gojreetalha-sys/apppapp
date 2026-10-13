package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.ReminderBroadcastReceiver
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(
        reminderId: Long,
        title: String,
        triggerTimeMillis: Long,
        ttsConfirmation: String
    ) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.example.ACTION_REMINDER_TRIGGER"
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_TTS_CONFIRMATION, ttsConfirmation)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fallback for strict background permission limitations
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(reminderId: Long) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.example.ACTION_REMINDER_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    companion object {
        fun parseIsoTimestamp(isoString: String): Long? {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val ta = DateTimeFormatter.ISO_DATE_TIME.parse(isoString)
                    Instant.from(ta).toEpochMilli()
                } else {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    sdf.parse(isoString)?.time
                }
            } catch (_: Exception) {
                try {
                    // Try alternative formats like yyyy-MM-dd HH:mm:ss
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    sdf.parse(isoString)?.time
                } catch (_: Exception) {
                    null
                }
            }
        }

        fun formatTimestamp(epochMillis: Long): String {
            val sdf = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            return sdf.format(Date(epochMillis))
        }
    }
}
