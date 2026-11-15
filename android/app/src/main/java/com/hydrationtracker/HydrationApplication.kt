package com.hydrationtracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.hydrationtracker.data.HydrationRepository
import com.hydrationtracker.data.hydrationStore
import com.hydrationtracker.reminder.ReminderScheduler
import com.hydrationtracker.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HydrationApplication : Application() {
    lateinit var repository: HydrationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = HydrationRepository(hydrationStore) {
            WidgetUpdater.updateAll(this)
        }
        ensureReminderChannel()
        AppScope.io.launch {
            val settings = repository.settings.first()
            if (settings.remindersEnabled) {
                ReminderScheduler.apply(this@HydrationApplication, settings)
            }
        }
    }
}

fun Context.ensureReminderChannel() {
    val manager = getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        getString(R.string.reminder_channel),
        NotificationManager.IMPORTANCE_DEFAULT,
    )
    channel.description = getString(R.string.reminder_channel_desc)
    manager.createNotificationChannel(channel)
}

const val REMINDER_CHANNEL_ID = "drink_reminders"
const val REMINDER_NOTIFICATION_ID = 1001
