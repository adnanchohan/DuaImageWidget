package com.watchfulai.duaimagewidget.data

import com.watchfulai.duaimagewidget.prayer.PrayerTimesWidgetProvider
import com.watchfulai.duaimagewidget.widget.DuaImageWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWidgetsTrackerTest {

    @Test
    fun knownProviders_containsDuaAndPrayerProviders() {
        val providers = ActiveWidgetsTracker.KNOWN_PROVIDERS

        assertEquals(2, providers.size)
        assertTrue(providers.contains(DuaImageWidgetReceiver::class.java))
        assertTrue(providers.contains(PrayerTimesWidgetProvider::class.java))
    }
}
