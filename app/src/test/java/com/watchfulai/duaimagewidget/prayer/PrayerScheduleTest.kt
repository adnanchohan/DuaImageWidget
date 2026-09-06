package com.watchfulai.duaimagewidget.prayer

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
    fun currentPrayer_respectsSunriseAsEndOfFajrWindow() {
        assertNull(schedule.currentPrayerAt(99L))
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
