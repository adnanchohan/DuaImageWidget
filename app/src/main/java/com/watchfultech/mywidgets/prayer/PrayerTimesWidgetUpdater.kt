package com.watchfultech.mywidgets.prayer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import com.watchfultech.mywidgets.R
import com.watchfultech.mywidgets.ui.prayer.PrayerWidgetConfigurationActivity
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

        val views = RemoteViews(context.packageName, R.layout.prayer_times_widget).apply {
            // Top Row: Hijri Date (Location shifted to hero tile)
            setTextViewText(
                R.id.prayer_hijri_date,
                formatHijriDate(context, schedule.hijriDateAt(now)),
            )

            // Hero Card: Next prayer countdown without seconds
            val diffMillis = (nextPrayer.epochMillis - now).coerceAtLeast(0L)
            val totalMinutes = (diffMillis + 59_999L) / 60_000L
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val timeRemaining = when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "< 1m"
            }
            val nextPrefix = context.getString(
                R.string.prayer_next_starts_in,
                nextPrayer.name.localizedName(context),
            ).trim()
            setTextViewText(R.id.prayer_next_label, "$nextPrefix $timeRemaining")

            // Hero Card: Location right before sunrise
            val sunriseTime = formatSimpleTime(context, schedule.sunriseEpochMillis, schedule.timezoneId)
            val sunriseText = context.getString(R.string.prayer_sunrise_value, sunriseTime)
            val location = config.locationLabel.trim()
            if (location.isNotEmpty()) {
                setTextViewText(R.id.prayer_location, location)
                setViewVisibility(R.id.prayer_sunrise_bullet, View.VISIBLE)
            } else {
                setTextViewText(R.id.prayer_location, "")
                setViewVisibility(R.id.prayer_sunrise_bullet, View.GONE)
            }
            setTextViewText(R.id.prayer_sunrise, sunriseText)

            // Bottom Slots: 5 prayer times with small am/pm
            schedule.prayers.forEach { prayer ->
                val viewIds = slotViewIds.getValue(prayer.name)
                setTextViewText(viewIds.name, prayer.name.localizedName(context))
                setTextViewText(
                    viewIds.time,
                    formatPrayerTimeWithSmallAmPm(context, prayer.epochMillis, schedule.timezoneId),
                )
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

            // Row 2: 4 Nafil prayer times (Ishraq, Chasht, Awwabin, Tahajjud)
            val currentNafil = schedule.currentNafilAt(now)
            schedule.nafils.forEach { nafil ->
                val viewIds = nafilSlotViewIds.getValue(nafil.name)
                setTextViewText(viewIds.name, nafil.name.localizedName(context))
                setTextViewText(
                    viewIds.time,
                    formatPrayerTimeWithSmallAmPm(context, nafil.epochMillis, schedule.timezoneId),
                )
                val isNafilActive = currentNafil == nafil.name
                setInt(
                    viewIds.container,
                    "setBackgroundResource",
                    if (isNafilActive) R.drawable.bg_prayer_slot_active else R.drawable.bg_prayer_slot,
                )
                val textColor = context.getColor(
                    if (isNafilActive) R.color.prayer_active_text else R.color.prayer_slot_text,
                )
                val secondaryColor = context.getColor(
                    if (isNafilActive) R.color.prayer_active_text else R.color.prayer_slot_text_secondary,
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

    private fun formatSimpleTime(context: Context, epochMillis: Long, timezoneId: String): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timezoneId)
        }.format(Date(epochMillis))
    }

    private fun formatPrayerTimeWithSmallAmPm(
        context: Context,
        epochMillis: Long,
        timezoneId: String,
    ): CharSequence {
        val date = Date(epochMillis)
        if (DateFormat.is24HourFormat(context)) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone(timezoneId)
            }
            return sdf.format(date)
        }

        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timezoneId)
        }
        val amPmFormat = SimpleDateFormat("a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timezoneId)
        }
        val timeStr = timeFormat.format(date)
        val amPmStr = amPmFormat.format(date).lowercase(Locale.getDefault())
        val fullStr = "$timeStr $amPmStr"
        val spannable = SpannableString(fullStr)
        val amPmStart = fullStr.length - amPmStr.length
        spannable.setSpan(
            RelativeSizeSpan(0.72f),
            amPmStart,
            fullStr.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return spannable
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

    private val nafilSlotViewIds = mapOf(
        NafilPrayerName.ISHRAQ to SlotViewIds(R.id.slot_ishraq, R.id.name_ishraq, R.id.time_ishraq),
        NafilPrayerName.CHASHT to SlotViewIds(R.id.slot_chasht, R.id.name_chasht, R.id.time_chasht),
        NafilPrayerName.AWWABIN to SlotViewIds(R.id.slot_awwabin, R.id.name_awwabin, R.id.time_awwabin),
        NafilPrayerName.TAHAJJUD to SlotViewIds(R.id.slot_tahajjud, R.id.name_tahajjud, R.id.time_tahajjud),
    )
}
