package com.watchfulai.mywidgets.prayer

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AlAdhanPrayerApi {
    fun fetchSchedule(
        config: PrayerWidgetConfig,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): PrayerSchedule {
        val deviceTimezone = TimeZone.getDefault()
        val calendar = Calendar.getInstance(deviceTimezone).apply { timeInMillis = nowEpochMillis }
        val todayKey = DATE_FORMAT.get()!!.apply { timeZone = deviceTimezone }.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrowKey = DATE_FORMAT.get()!!.apply { timeZone = deviceTimezone }.format(calendar.time)

        val todayMonth = calendarParts(todayKey)
        val tomorrowMonth = calendarParts(tomorrowKey)
        val currentMonthData = requestMonth(config, todayMonth.year, todayMonth.month)
        val today = currentMonthData.day(todayKey)
            ?: throw IOException("AlAdhan did not return $todayKey")
        val tomorrow = if (todayMonth.month == tomorrowMonth.month && todayMonth.year == tomorrowMonth.year) {
            currentMonthData.day(tomorrowKey)
        } else {
            requestMonth(config, tomorrowMonth.year, tomorrowMonth.month).day(tomorrowKey)
        } ?: throw IOException("AlAdhan did not return $tomorrowKey")

        return parseSchedule(today, tomorrow)
    }

    private fun requestMonth(config: PrayerWidgetConfig, year: Int, month: Int): MonthResponse {
        val query = buildList {
            add("latitude=${encode(config.latitude.toString())}")
            add("longitude=${encode(config.longitude.toString())}")
            add("school=${config.asrSchool}")
            add("latitudeAdjustmentMethod=3")
            if (config.calculationMethodId != AUTOMATIC_CALCULATION_METHOD) {
                add("method=${config.calculationMethodId}")
            }
        }.joinToString("&")
        val url = URL("$BASE_URL/calendar/$year/$month?$query")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MILLIS
            readTimeout = NETWORK_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "DuaImageWidget/1.0 (Android)")
            useCaches = true
        }

        return try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("AlAdhan returned HTTP $status")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            if (root.optInt("code") != 200) {
                throw IOException("AlAdhan response was not successful")
            }
            MonthResponse(root.getJSONArray("data"))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSchedule(today: JSONObject, tomorrow: JSONObject): PrayerSchedule {
        val metadata = today.getJSONObject("meta")
        val timezoneId = metadata.getString("timezone")
        val timezone = TimeZone.getTimeZone(timezoneId)
        val date = today.getJSONObject("date")
        val dateKey = date.getJSONObject("gregorian").getString("date")
        val timings = today.getJSONObject("timings")
        val tomorrowTimings = tomorrow.getJSONObject("timings")
        val tomorrowDate = tomorrow.getJSONObject("date")
        val tomorrowKey = tomorrowDate.getJSONObject("gregorian")
            .getString("date")
        val hijri = date.getJSONObject("hijri")
        val tomorrowHijri = tomorrowDate.getJSONObject("hijri")

        fun prayer(name: PrayerName, key: String) = PrayerMoment(
            name = name,
            epochMillis = parsePrayerTime(dateKey, timings.getString(key), timezone),
        )

        return PrayerSchedule(
            dateKey = dateKey,
            timezoneId = timezoneId,
            hijriDate = HijriDate(
                day = hijri.getString("day").toInt(),
                month = hijri.getJSONObject("month").getInt("number"),
                year = hijri.getString("year").toInt(),
            ),
            tomorrowHijriDate = HijriDate(
                day = tomorrowHijri.getString("day").toInt(),
                month = tomorrowHijri.getJSONObject("month").getInt("number"),
                year = tomorrowHijri.getString("year").toInt(),
            ),
            sunriseEpochMillis = parsePrayerTime(
                dateKey,
                timings.getString("Sunrise"),
                timezone,
            ),
            prayers = listOf(
                prayer(PrayerName.FAJR, "Fajr"),
                prayer(PrayerName.DHUHR, "Dhuhr"),
                prayer(PrayerName.ASR, "Asr"),
                prayer(PrayerName.MAGHRIB, "Maghrib"),
                prayer(PrayerName.ISHA, "Isha"),
            ),
            tomorrowFajrEpochMillis = parsePrayerTime(
                tomorrowKey,
                tomorrowTimings.getString("Fajr"),
                timezone,
            ),
            calculationMethod = metadata.getJSONObject("method").getString("name"),
        )
    }

    private fun parsePrayerTime(date: String, encodedTime: String, timezone: TimeZone): Long {
        val match = TIME_PATTERN.find(encodedTime)
            ?: throw IOException("Invalid prayer time: $encodedTime")
        val value = "$date ${match.groupValues[1]}:${match.groupValues[2]}"
        return DATE_TIME_FORMAT.get()!!.apply { timeZone = timezone }.parse(value)?.time
            ?: throw IOException("Could not parse prayer time: $encodedTime")
    }

    private fun calendarParts(dateKey: String): CalendarParts {
        val parsed = DATE_FORMAT.get()!!.parse(dateKey) ?: Date()
        return Calendar.getInstance().run {
            time = parsed
            CalendarParts(
                year = get(Calendar.YEAR),
                month = get(Calendar.MONTH) + 1,
            )
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private class MonthResponse(private val days: JSONArray) {
        fun day(dateKey: String): JSONObject? {
            for (index in 0 until days.length()) {
                val day = days.getJSONObject(index)
                val encodedDate = day.getJSONObject("date")
                    .getJSONObject("gregorian")
                    .getString("date")
                if (encodedDate == dateKey) return day
            }
            return null
        }
    }

    private data class CalendarParts(val year: Int, val month: Int)

    private companion object {
        const val BASE_URL = "https://api.aladhan.com/v1"
        const val NETWORK_TIMEOUT_MILLIS = 8_000
        val TIME_PATTERN = Regex("(\\d{1,2}):(\\d{2})")
        val DATE_FORMAT = ThreadLocal.withInitial {
            SimpleDateFormat("dd-MM-yyyy", Locale.US).apply { isLenient = false }
        }
        val DATE_TIME_FORMAT = ThreadLocal.withInitial {
            SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).apply { isLenient = false }
        }
    }
}
