package com.watchfulai.mywidgets.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.watchfulai.mywidgets.prayer.PrayerTimesWidgetProvider
import com.watchfulai.mywidgets.widget.DuaImageWidgetReceiver

object ActiveWidgetsTracker {
    val KNOWN_PROVIDERS: List<Class<*>> = listOf(
        DuaImageWidgetReceiver::class.java,
        PrayerTimesWidgetProvider::class.java,
        com.watchfulai.mywidgets.tasbeeh.TasbeehWidgetProvider::class.java,
    )

    fun getTotalActiveWidgetCount(context: Context): Int {
        val manager = AppWidgetManager.getInstance(context)
        return KNOWN_PROVIDERS.sumOf { providerClass ->
            manager.getAppWidgetIds(ComponentName(context, providerClass)).size
        }
    }

    fun getWidgetIdsForProvider(context: Context, providerClass: Class<*>): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        return manager.getAppWidgetIds(ComponentName(context, providerClass))
    }

    fun getDuaWidgetIds(context: Context): IntArray =
        getWidgetIdsForProvider(context, DuaImageWidgetReceiver::class.java)

    fun getPrayerWidgetIds(context: Context): IntArray =
        getWidgetIdsForProvider(context, PrayerTimesWidgetProvider::class.java)
}
