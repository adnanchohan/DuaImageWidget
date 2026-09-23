package com.watchfulai.mywidgets.prayer

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrayerTimesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updateAsync(context, appWidgetIds, allowNetwork = true)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAsync(context, intArrayOf(appWidgetId), allowNetwork = false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_PRAYER_WIDGET -> {
                val id = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateAsync(context, intArrayOf(id), allowNetwork = true)
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            -> updateAsync(context, activeWidgetIds(context), allowNetwork = true)

            Intent.ACTION_CONFIGURATION_CHANGED ->
                updateAsync(context, activeWidgetIds(context), allowNetwork = false)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val repository = PrayerWidgetRepository(context)
        appWidgetIds.forEach { id ->
            repository.remove(id)
            PrayerWidgetScheduler.cancel(context, id)
        }
    }

    private fun updateAsync(context: Context, ids: IntArray, allowNetwork: Boolean) {
        if (ids.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ids.forEach { id ->
                    PrayerTimesWidgetUpdater.update(context.applicationContext, id, allowNetwork)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun activeWidgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, PrayerTimesWidgetProvider::class.java),
        )
}
