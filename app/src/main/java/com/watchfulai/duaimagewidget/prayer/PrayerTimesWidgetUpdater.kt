package com.watchfulai.duaimagewidget.prayer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import com.watchfulai.duaimagewidget.R
import com.watchfulai.duaimagewidget.ui.prayer.PrayerWidgetConfigurationActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PrayerTimesWidgetUpdater {
    fun update(
        context: Context,
        appWidgetId: Int,
        allowNetwork: Boolean,
    ) {
        val repository = PrayerWidgetRepository(context)
        val config = repository.getConfig(appWidgetId)
        if (config == null) {
            showSetupRequired(context, appWidgetId)
            return
        }

        val cached = repository.getSchedule(appWidgetId)
        val schedule = if (allowNetwork && !isForToday(cached)) {
            runCatching { AlAdhanPrayerApi().fetchSchedule(config) }
                .onSuccess { repository.saveSchedule(appWidgetId, it) }
                .getOrNull() ?: cached
        } else {
            cached
        }

        if (schedule == null) {
            showUpdateError(context, appWidgetId, config.locationLabel)
            return
        }

        render(context, appWidgetId, config, schedule)
        PrayerWidgetScheduler.schedule(context, appWidgetId, schedule)
    }

    private fun render(
        context: Context,
        appWidgetId: Int,
        config: PrayerWidgetConfig,
        schedule: PrayerSchedule,
    ) {
        val now = System.currentTimeMillis()
        val currentPrayer = schedule.currentPrayerAt(now)
        val nextPrayer = schedule.nextPrayerAt(now)
        val timeFormatter = widgetTimeFormatter(context, schedule.timezoneId)
        val views = RemoteViews(context.packageName, R.layout.prayer_times_widget).apply {
            setTextViewText(R.id.prayer_location, config.locationLabel)
            setTextViewText(
                R.id.prayer_hijri_date,
                formatHijriDate(context, schedule.hijriDateAt(now)),
            )
            setTextViewText(
                R.id.prayer_sunrise,
                context.getString(
                    R.string.prayer_sunrise_value,
                    timeFormatter.format(Date(schedule.sunriseEpochMillis)),
                ),
            )
            setTextViewText(
                R.id.prayer_next_label,
                context.getString(
                    R.string.prayer_next_starts_in,
                    nextPrayer.name.localizedName(context),
                ),
            )
            setChronometer(
                R.id.prayer_countdown,
                SystemClock.elapsedRealtime() + (nextPrayer.epochMillis - now).coerceAtLeast(0L),
                null,
                true,
            )
            setChronometerCountDown(R.id.prayer_countdown, true)

            schedule.prayers.forEach { prayer ->
                val viewIds = slotViewIds.getValue(prayer.name)
                setTextViewText(viewIds.name, prayer.name.localizedName(context))
                setTextViewText(viewIds.time, timeFormatter.format(Date(prayer.epochMillis)))
                val isActive = currentPrayer == prayer.name
                setInt(
                    viewIds.container,
                    "setBackgroundResource",
                    if (isActive) R.drawable.bg_prayer_slot_active else R.drawable.bg_prayer_slot,
                )
                val textColor = context.getColor(
                    if (isActive) R.color.prayer_active_text else R.color.prayer_slot_text,
                )
                val secondaryColor = context.getColor(
                    if (isActive) R.color.prayer_active_text else R.color.prayer_slot_text_secondary,
                )
                setTextColor(viewIds.name, secondaryColor)
                setTextColor(viewIds.time, textColor)
            }

            setOnClickPendingIntent(
                R.id.prayer_widget_root,
                configurationPendingIntent(context, appWidgetId),
            )
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }

    private fun showSetupRequired(context: Context, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.prayer_times_widget_status).apply {
            setTextViewText(R.id.prayer_status_title, context.getString(R.string.prayer_setup_title))
            setTextViewText(R.id.prayer_status_message, context.getString(R.string.prayer_setup_message))
            setViewVisibility(R.id.prayer_status_progress, View.GONE)
            setOnClickPendingIntent(
                R.id.prayer_status_root,
                configurationPendingIntent(context, appWidgetId),
            )
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }

    private fun showUpdateError(context: Context, appWidgetId: Int, location: String) {
        val views = RemoteViews(context.packageName, R.layout.prayer_times_widget_status).apply {
            setTextViewText(R.id.prayer_status_title, context.getString(R.string.prayer_update_failed))
            setTextViewText(
                R.id.prayer_status_message,
                context.getString(R.string.prayer_update_failed_message, location),
            )
            setViewVisibility(R.id.prayer_status_progress, View.GONE)
            setOnClickPendingIntent(
                R.id.prayer_status_root,
                configurationPendingIntent(context, appWidgetId),
            )
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }

    private fun isForToday(schedule: PrayerSchedule?): Boolean {
        schedule ?: return false
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(schedule.timezoneId)
        }
        return schedule.dateKey == formatter.format(Date())
    }

    private fun widgetTimeFormatter(context: Context, timezoneId: String): SimpleDateFormat {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timezoneId)
        }
    }

    private fun formatHijriDate(context: Context, date: HijriDate): String {
        val monthNames = context.resources.getStringArray(R.array.hijri_month_names)
        val month = monthNames.getOrElse(date.month - 1) { date.month.toString() }
        return context.getString(R.string.prayer_hijri_date_format, date.day, month, date.year)
    }

    private fun configurationPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, PrayerWidgetConfigurationActivity::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private data class SlotViewIds(
        val container: Int,
        val name: Int,
        val time: Int,
    )

    private val slotViewIds = mapOf(
        PrayerName.FAJR to SlotViewIds(R.id.slot_fajr, R.id.name_fajr, R.id.time_fajr),
        PrayerName.DHUHR to SlotViewIds(R.id.slot_dhuhr, R.id.name_dhuhr, R.id.time_dhuhr),
        PrayerName.ASR to SlotViewIds(R.id.slot_asr, R.id.name_asr, R.id.time_asr),
        PrayerName.MAGHRIB to SlotViewIds(R.id.slot_maghrib, R.id.name_maghrib, R.id.time_maghrib),
        PrayerName.ISHA to SlotViewIds(R.id.slot_isha, R.id.name_isha, R.id.time_isha),
    )
}
