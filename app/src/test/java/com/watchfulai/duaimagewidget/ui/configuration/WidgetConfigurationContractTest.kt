package com.watchfulai.duaimagewidget.ui.configuration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun androidDpDimensionsArePresentedAsLauncherCellSpans() {
        assertEquals(
            WidgetCellSize(columns = 4, rows = 2),
            WidgetSizeDp(width = 250f, height = 110f).toWidgetCellSize(),
        )
        assertEquals(
            WidgetCellSize(columns = 2, rows = 2),
            WidgetSizeDp(width = 40f, height = 40f).toWidgetCellSize(),
        )
    }

    @Test
    fun launcherAllocatedFourthColumnIsNotRoundedUpToFive() {
        assertEquals(
            WidgetCellSize(columns = 4, rows = 2),
            WidgetSizeDp(width = 293f, height = 117.666664f).toWidgetCellSize(),
        )
        assertEquals(
            WidgetCellSize(columns = 3, rows = 2),
            WidgetSizeDp(width = 190f, height = 117.666664f).toWidgetCellSize(),
        )
    }

    @Test
    fun currentCellSizeIsFirstAndStandardSizesAreUnique() {
        val current = WidgetCellSize(columns = 4, rows = 2)
        val sizes = availableWidgetCellSizes(current)

        assertEquals(current, sizes.first())
        assertEquals(sizes.size, sizes.distinct().size)
        assertEquals(16, sizes.size)
        assertTrue(sizes.all { it.columns in 2..5 && it.rows in 2..5 })
    }

    @Test
    fun launcherCellSpanProducesAndroidPreviewDimensions() {
        val preview = WidgetCellSize(columns = 4, rows = 2).toPreviewSizeDp()

        assertEquals(250f, preview.width, 0f)
        assertEquals(110f, preview.height, 0f)
    }
}
