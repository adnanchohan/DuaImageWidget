package com.watchfultech.mywidgets.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerScheduleTest {
    private val schedule = PrayerSchedule(
        dateKey = "30-08-2026",
        timezoneId = "Asia/Karachi",
        hijriDate = HijriDate(17, 3, 1448),
        tomorrowHijriDate = HijriDate(18, 3, 1448),
        sunriseEpochMillis = 200L,
        prayers = listOf(
            PrayerMoment(PrayerName.FAJR, 100L),
            PrayerMoment(PrayerName.DHUHR, 300L),
            PrayerMoment(PrayerName.ASR, 400L),
            PrayerMoment(PrayerName.MAGHRIB, 500L),
            PrayerMoment(PrayerName.ISHA, 600L),
        ),
        tomorrowFajrEpochMillis = 700L,
        calculationMethod = "University of Islamic Sciences, Karachi",
    )

    @Test
    fun currentPrayer_fajrUntilSunriseAndDhuhrAtDhuhr() {
        assertEquals(PrayerName.ISHA, schedule.currentPrayerAt(99L))
        assertEquals(PrayerName.FAJR, schedule.currentPrayerAt(100L))
        assertEquals(PrayerName.FAJR, schedule.currentPrayerAt(199L))
        assertNull(schedule.currentPrayerAt(200L))
        assertNull(schedule.currentPrayerAt(299L))
        assertEquals(PrayerName.DHUHR, schedule.currentPrayerAt(300L))
        assertEquals(PrayerName.ASR, schedule.currentPrayerAt(400L))
        assertEquals(PrayerName.MAGHRIB, schedule.currentPrayerAt(500L))
        assertEquals(PrayerName.ISHA, schedule.currentPrayerAt(600L))
    }

    @Test
    fun nafilPrayers_calculatedProperly() {
        val nafils = schedule.nafils
        assertEquals(4, nafils.size)
        val ishraq = nafils.first { it.name == NafilPrayerName.ISHRAQ }
        val chasht = nafils.first { it.name == NafilPrayerName.CHASHT }
        val awwabin = nafils.first { it.name == NafilPrayerName.AWWABIN }
        val tahajjud = nafils.first { it.name == NafilPrayerName.TAHAJJUD }

        assertEquals(200L + 15 * 60_000L, ishraq.epochMillis)
        assertEquals(200L + (500L - 200L) / 4, chasht.epochMillis)
        assertEquals(500L + 15 * 60_000L, awwabin.epochMillis)
        assertEquals(500L + (2 * (700L - 500L) / 3), tahajjud.epochMillis)
    }

    @Test
    fun nextPrayer_rollsToTomorrowFajrAfterIsha() {
        assertEquals(PrayerMoment(PrayerName.FAJR, 100L), schedule.nextPrayerAt(0L))
        assertEquals(PrayerMoment(PrayerName.DHUHR, 300L), schedule.nextPrayerAt(100L))
        assertEquals(PrayerMoment(PrayerName.FAJR, 700L), schedule.nextPrayerAt(600L))
    }

    @Test
    fun hijriDate_changesAtMaghrib() {
        assertEquals(schedule.hijriDate, schedule.hijriDateAt(499L))
        assertEquals(schedule.tomorrowHijriDate, schedule.hijriDateAt(500L))
    }
}
