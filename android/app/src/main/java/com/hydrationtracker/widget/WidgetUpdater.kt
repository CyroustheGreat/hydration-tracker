package com.hydrationtracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hydrationtracker.AppScope
import com.hydrationtracker.HydrationApplication
import com.hydrationtracker.MainActivity
import com.hydrationtracker.R
import com.hydrationtracker.domain.HydrationMath
import com.hydrationtracker.domain.Volume
import com.hydrationtracker.reminder.ReminderReceiver
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        AppScope.io.launch {
            val app = appContext as HydrationApplication
            val settings = app.repository.settings.first()
            val entries = app.repository.entries.first()
            val today = LocalDate.now(ZoneId.systemDefault())
            val total = HydrationMath.dailyTotalMl(HydrationMath.entriesForDate(entries, today))
            val percent = HydrationMath.progressPercent(total, settings.goalMl)
            val views = RemoteViews(appContext.packageName, R.layout.widget_hydration).apply {
                setTextViewText(R.id.widget_amount, Volume.formatAmount(total, settings.unit))
                setTextViewText(
                    R.id.widget_goal,
                    appContext.getString(
                        R.string.percent_of_goal,
                        percent,
                        Volume.formatAmount(settings.goalMl, settings.unit),
                    ),
                )
                setProgressBar(R.id.widget_progress, 100, percent, false)
                setTextViewText(
                    R.id.widget_add,
                    appContext.getString(R.string.widget_add_glass, Volume.formatAmount(settings.glassMl, settings.unit)),
                )
                setOnClickPendingIntent(R.id.widget_root, openApp(appContext))
                setOnClickPendingIntent(R.id.widget_add, addGlass(appContext))
            }
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, HydrationWidgetProvider::class.java))
            ids.forEach { id -> manager.updateAppWidget(id, views) }
        }
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun addGlass(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_ADD_GLASS)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_ADD,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val REQUEST_OPEN = 4
    private const val REQUEST_ADD = 5
}
