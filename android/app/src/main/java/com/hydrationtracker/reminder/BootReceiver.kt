package com.hydrationtracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hydrationtracker.AppScope
import com.hydrationtracker.HydrationApplication
import com.hydrationtracker.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores the next drink reminder after reboot, clock changes, and app updates.
 * Work is explicit so a force-stop is the only thing that clears it, which is
 * Android's normal rule for background alarms.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as HydrationApplication
        AppScope.io.launch {
            try {
                val settings = app.repository.settings.first()
                ReminderScheduler.apply(app, settings)
                WidgetUpdater.updateAll(app)
            } finally {
                pending.finish()
            }
        }
    }
}
