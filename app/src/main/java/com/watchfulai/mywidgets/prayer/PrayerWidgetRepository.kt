package com.watchfulai.mywidgets.prayer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PrayerWidgetRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun getConfig(appWidgetId: Int): PrayerWidgetConfig? =
        preferences.getString(configKey(appWidgetId), null)?.let(::decodeConfig)

    fun saveConfig(config: PrayerWidgetConfig) {
        preferences.edit()
            .putString(configKey(config.appWidgetId), encodeConfig(config).toString())
            .apply()
    }

    fun getSchedule(appWidgetId: Int): PrayerSchedule? =
        preferences.getString(scheduleKey(appWidgetId), null)?.let(::decodeSchedule)

    fun saveSchedule(appWidgetId: Int, schedule: PrayerSchedule) {
        preferences.edit()
            .putString(scheduleKey(appWidgetId), encodeSchedule(schedule).toString())
            .apply()
    }

    fun remove(appWidgetId: Int) {
        preferences.edit()
            .remove(configKey(appWidgetId))
            .remove(scheduleKey(appWidgetId))
            .apply()
    }

    private fun encodeConfig(config: PrayerWidgetConfig) = JSONObject().apply {
        put("appWidgetId", config.appWidgetId)
        put("latitude", config.latitude)
        put("longitude", config.longitude)
        put("locationLabel", config.locationLabel)
        put("calculationMethodId", config.calculationMethodId)
        put("asrSchool", config.asrSchool)
    }

    private fun decodeConfig(value: String): PrayerWidgetConfig? = runCatching {
        val json = JSONObject(value)
        PrayerWidgetConfig(
            appWidgetId = json.getInt("appWidgetId"),
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
            locationLabel = json.getString("locationLabel"),
            calculationMethodId = json.optInt(
                "calculationMethodId",
                AUTOMATIC_CALCULATION_METHOD,
            ),
            asrSchool = json.optInt("asrSchool", SHAFI_ASR_SCHOOL),
        )
    }.getOrNull()

    private fun encodeSchedule(schedule: PrayerSchedule) = JSONObject().apply {
        put("dateKey", schedule.dateKey)
        put("timezoneId", schedule.timezoneId)
        put("hijriDay", schedule.hijriDate.day)
        put("hijriMonth", schedule.hijriDate.month)
        put("hijriYear", schedule.hijriDate.year)
        put("tomorrowHijriDay", schedule.tomorrowHijriDate.day)
        put("tomorrowHijriMonth", schedule.tomorrowHijriDate.month)
        put("tomorrowHijriYear", schedule.tomorrowHijriDate.year)
        put("sunrise", schedule.sunriseEpochMillis)
        put("tomorrowFajr", schedule.tomorrowFajrEpochMillis)
        put("calculationMethod", schedule.calculationMethod)
        put("fetchedAt", schedule.fetchedAtEpochMillis)
        put("prayers", JSONArray().apply {
            schedule.prayers.forEach { prayer ->
                put(JSONObject().apply {
                    put("name", prayer.name.name)
                    put("time", prayer.epochMillis)
                })
            }
        })
    }

    private fun decodeSchedule(value: String): PrayerSchedule? = runCatching {
        val json = JSONObject(value)
        val encodedPrayers = json.getJSONArray("prayers")
        val prayers = buildList {
            for (index in 0 until encodedPrayers.length()) {
                val prayer = encodedPrayers.getJSONObject(index)
                add(
                    PrayerMoment(
                        name = PrayerName.valueOf(prayer.getString("name")),
                        epochMillis = prayer.getLong("time"),
                    ),
                )
            }
        }
        PrayerSchedule(
            dateKey = json.getString("dateKey"),
            timezoneId = json.getString("timezoneId"),
            hijriDate = HijriDate(
                day = json.getInt("hijriDay"),
                month = json.getInt("hijriMonth"),
                year = json.getInt("hijriYear"),
            ),
            tomorrowHijriDate = HijriDate(
                day = json.getInt("tomorrowHijriDay"),
                month = json.getInt("tomorrowHijriMonth"),
                year = json.getInt("tomorrowHijriYear"),
            ),
            sunriseEpochMillis = json.getLong("sunrise"),
            prayers = prayers,
            tomorrowFajrEpochMillis = json.getLong("tomorrowFajr"),
            calculationMethod = json.getString("calculationMethod"),
            fetchedAtEpochMillis = json.optLong("fetchedAt", 0L),
        )
    }.getOrNull()

    private fun configKey(id: Int) = "prayer_widget_${id}_config"
    private fun scheduleKey(id: Int) = "prayer_widget_${id}_schedule"

    private companion object {
        const val PREFERENCES_NAME = "prayer_widget_data"
    }
}
