package com.hydrationtracker.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hydrationtracker.AppScope
import com.hydrationtracker.HydrationApplication
import com.hydrationtracker.MainActivity
import com.hydrationtracker.REMINDER_CHANNEL_ID
import com.hydrationtracker.REMINDER_NOTIFICATION_ID
import com.hydrationtracker.R
import com.hydrationtracker.domain.DrinkType
import com.hydrationtracker.domain.HydrationMath
import com.hydrationtracker.domain.ReminderSchedule
import com.hydrationtracker.domain.Volume
import com.hydrationtracker.ensureReminderChannel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as HydrationApplication
        AppScope.io.launch {
            try {
                when (intent.action) {
                    ACTION_ALARM -> handleAlarm(app)
                    ACTION_ADD_GLASS -> {
                        val settings = app.repository.settings.first()
                        app.repository.addDrink(settings.glassMl, DrinkType.WATER)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleAlarm(app: HydrationApplication) {
        val settings = app.repository.settings.first()
        if (!settings.remindersEnabled) {
            ReminderScheduler.apply(app, settings)
            return
        }
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val start = minutesToTime(settings.reminderStartMinuteOfDay)
        val end = minutesToTime(settings.reminderEndMinuteOfDay)
        if (ReminderSchedule.isWithinWindow(now.toLocalTime(), start, end)) {
            val today = LocalDate.now(zone)
            val entries = app.repository.entries.first()
            val total = HydrationMath.dailyTotalMl(HydrationMath.entriesForDate(entries, today))
            showNotification(app, settings.goalMl, settings.unit, total, settings.glassMl)
        }
        ReminderScheduler.apply(app, settings)
    }

    private fun showNotification(
        context: Context,
        goalMl: Int,
        unit: com.hydrationtracker.domain.VolumeUnit,
        totalMl: Int,
        glassMl: Int,
    ) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        context.ensureReminderChannel()
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val addGlass = PendingIntent.getBroadcast(
            context,
            REQUEST_ADD,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_ADD_GLASS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = if (HydrationMath.goalReached(totalMl, goalMl)) {
            context.getString(R.string.reminder_goal_reached)
        } else {
            context.getString(
                R.string.reminder_body,
                Volume.formatAmount(totalMl, unit),
                Volume.formatAmount(goalMl, unit),
            )
        }
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_water)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notification_add_glass, Volume.formatAmount(glassMl, unit)), addGlass)
            .build()
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    private fun minutesToTime(minuteOfDay: Int): LocalTime {
        val minutes = ((minuteOfDay % (24 * 60)) + (24 * 60)) % (24 * 60)
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    companion object {
        const val ACTION_ALARM = "com.hydrationtracker.action.REMIND"
        const val ACTION_ADD_GLASS = "com.hydrationtracker.action.ADD_GLASS"
        private const val REQUEST_OPEN = 2
        private const val REQUEST_ADD = 3
    }
}
