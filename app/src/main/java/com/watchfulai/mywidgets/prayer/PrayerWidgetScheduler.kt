package com.watchfulai.mywidgets.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

object PrayerWidgetScheduler {
    fun schedule(context: Context, appWidgetId: Int, schedule: PrayerSchedule) {
        val now = System.currentTimeMillis()
        val nextPrayer = schedule.nextPrayerAt(now).epochMillis + TRANSITION_GRACE_MILLIS
        val nextMidnight = Calendar.getInstance(TimeZone.getTimeZone(schedule.timezoneId)).run {
            timeInMillis = now
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
        val triggerAt = minOf(nextPrayer, nextMidnight).coerceAtLeast(now + MINIMUM_DELAY_MILLIS)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = refreshPendingIntent(context, appWidgetId)

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancel(context: Context, appWidgetId: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(
            refreshPendingIntent(context, appWidgetId),
        )
    }

    private fun refreshPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            appWidgetId,
            Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_PRAYER_WIDGET
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private const val TRANSITION_GRACE_MILLIS = 2_000L
    private const val MINIMUM_DELAY_MILLIS = 5_000L
}

const val ACTION_REFRESH_PRAYER_WIDGET =
    "com.watchfulai.mywidgets.action.REFRESH_PRAYER_WIDGET"
