package com.watchfulai.mywidgets.data

import com.watchfulai.mywidgets.prayer.PrayerTimesWidgetProvider
import com.watchfulai.mywidgets.widget.DuaImageWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWidgetsTrackerTest {

    @Test
    fun knownProviders_containsAllProviders() {
        val providers = ActiveWidgetsTracker.KNOWN_PROVIDERS

        assertEquals(3, providers.size)
        assertTrue(providers.contains(DuaImageWidgetReceiver::class.java))
        assertTrue(providers.contains(PrayerTimesWidgetProvider::class.java))
        assertTrue(providers.contains(com.watchfulai.mywidgets.tasbeeh.TasbeehWidgetProvider::class.java))
    }
}
