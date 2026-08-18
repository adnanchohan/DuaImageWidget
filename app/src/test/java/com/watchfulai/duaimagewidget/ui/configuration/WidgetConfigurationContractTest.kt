package com.watchfulai.duaimagewidget.ui.configuration

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetConfigurationContractTest {
    @Test
    fun exactWidgetSizeIsPreservedWithoutAspectRatioClamping() {
        val resolved = resolveWidgetSize(
            exactSize = WidgetSizeDp(width = 40f, height = 300f),
            minWidth = 250,
            minHeight = 110,
            maxWidth = 600,
            maxHeight = 400,
            isLandscape = false,
        )

        assertEquals(40f, resolved.width, 0f)
        assertEquals(300f, resolved.height, 0f)
    }

    @Test
    fun portraitFallbackUsesCurrentPortraitBounds() {
        val resolved = resolveWidgetSize(
            exactSize = null,
            minWidth = 120,
            minHeight = 80,
            maxWidth = 240,
            maxHeight = 180,
            isLandscape = false,
        )

        assertEquals(120f, resolved.width, 0f)
        assertEquals(180f, resolved.height, 0f)
    }

    @Test
    fun landscapeFallbackUsesCurrentLandscapeBounds() {
        val resolved = resolveWidgetSize(
            exactSize = null,
            minWidth = 120,
            minHeight = 80,
            maxWidth = 240,
            maxHeight = 180,
            isLandscape = true,
        )

        assertEquals(240f, resolved.width, 0f)
        assertEquals(80f, resolved.height, 0f)
    }
}
