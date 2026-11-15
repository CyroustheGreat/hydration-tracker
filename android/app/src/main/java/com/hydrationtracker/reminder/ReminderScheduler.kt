package com.hydrationtracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hydrationtracker.domain.ReminderSchedule
import com.hydrationtracker.domain.TrackerSettings
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    fun apply(context: Context, settings: TrackerSettings) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = alarmPendingIntent(context)
        if (!settings.remindersEnabled) {
            alarmManager.cancel(pending)
            return
        }
        val zone = ZoneId.systemDefault()
        val next = ReminderSchedule.nextTrigger(
            from = LocalDateTime.now(zone),
            start = minutesToTime(settings.reminderStartMinuteOfDay),
            end = minutesToTime(settings.reminderEndMinuteOfDay),
            intervalMinutes = settings.reminderIntervalMinutes.coerceAtLeast(15),
        )
        val triggerAt = next.atZone(zone).toInstant().toEpochMilli()
        if (canUseExactAlarms(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // Exact alarms need a user grant on Android 12+. Inexact alarms still survive reboot.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun canUseExactAlarms(context: Context): Boolean =
        canUseExactAlarms(context.getSystemService(AlarmManager::class.java))

    private fun canUseExactAlarms(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_ALARM)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun minutesToTime(minuteOfDay: Int): LocalTime {
        val minutes = ((minuteOfDay % (24 * 60)) + (24 * 60)) % (24 * 60)
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    private const val REQUEST_ALARM = 1
}
