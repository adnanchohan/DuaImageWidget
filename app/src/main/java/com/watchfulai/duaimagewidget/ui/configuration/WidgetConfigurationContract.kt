package com.watchfulai.duaimagewidget.ui.configuration

import android.content.Intent

internal data class WidgetSizeDp(
    val width: Float,
    val height: Float,
) {
    val isValid: Boolean
        get() = width.isFinite() && height.isFinite() && width > 0f && height > 0f
}

internal data class WidgetCellSize(
    val columns: Int,
    val rows: Int,
) {
    val label: String
        get() = "\u2066$columns × $rows\u2069"
}

internal fun WidgetSizeDp.toWidgetCellSize(): WidgetCellSize = WidgetCellSize(
    columns = width.toWidgetCellCount(),
    rows = height.toWidgetCellCount(),
)

internal fun WidgetCellSize.toPreviewSizeDp(): WidgetSizeDp = WidgetSizeDp(
    width = columns * LEGACY_WIDGET_CELL_DP - LEGACY_WIDGET_GUTTER_DP,
    height = rows * LEGACY_WIDGET_CELL_DP - LEGACY_WIDGET_GUTTER_DP,
)

internal fun availableWidgetCellSizes(current: WidgetCellSize): List<WidgetCellSize> {
    val standardSizes = buildList {
        for (rows in MIN_STANDARD_WIDGET_SPAN..MAX_STANDARD_WIDGET_SPAN) {
            for (columns in MIN_STANDARD_WIDGET_SPAN..MAX_STANDARD_WIDGET_SPAN) {
                add(WidgetCellSize(columns = columns, rows = rows))
            }
        }
    }
    return if (current in standardSizes) {
        listOf(current) + standardSizes.filterNot { it == current }
    } else {
        standardSizes
    }
}

internal fun Intent.putWidgetSize(size: WidgetSizeDp): Intent = apply {
    putExtra(EXTRA_WIDGET_WIDTH_DP, size.width)
    putExtra(EXTRA_WIDGET_HEIGHT_DP, size.height)
}

internal fun Intent.widgetSizeOrNull(): WidgetSizeDp? {
    val size = WidgetSizeDp(
        width = getFloatExtra(EXTRA_WIDGET_WIDTH_DP, Float.NaN),
        height = getFloatExtra(EXTRA_WIDGET_HEIGHT_DP, Float.NaN),
    )
    return size.takeIf(WidgetSizeDp::isValid)
}

internal const val EXTRA_EDIT_FROM_WIDGET_LIST =
    "com.watchfulai.duaimagewidget.extra.EDIT_FROM_WIDGET_LIST"

internal fun resolveWidgetSize(
    exactSize: WidgetSizeDp?,
    minWidth: Int,
    minHeight: Int,
    maxWidth: Int,
    maxHeight: Int,
    isLandscape: Boolean,
): WidgetSizeDp {
    exactSize?.takeIf(WidgetSizeDp::isValid)?.let { return it }

    val width = if (isLandscape) {
        maxWidth.takeIf { it > 0 } ?: minWidth
    } else {
        minWidth.takeIf { it > 0 } ?: maxWidth
    }
    val height = if (isLandscape) {
        minHeight.takeIf { it > 0 } ?: maxHeight
    } else {
        maxHeight.takeIf { it > 0 } ?: minHeight
    }

    return WidgetSizeDp(
        width = width.takeIf { it > 0 }?.toFloat() ?: DEFAULT_WIDGET_WIDTH_DP,
        height = height.takeIf { it > 0 }?.toFloat() ?: DEFAULT_WIDGET_HEIGHT_DP,
    )
}

private const val EXTRA_WIDGET_WIDTH_DP =
    "com.watchfulai.duaimagewidget.extra.WIDGET_WIDTH_DP"
private const val EXTRA_WIDGET_HEIGHT_DP =
    "com.watchfulai.duaimagewidget.extra.WIDGET_HEIGHT_DP"
private const val DEFAULT_WIDGET_WIDTH_DP = 250f
private const val DEFAULT_WIDGET_HEIGHT_DP = 110f

private fun Float.toWidgetCellCount(): Int =
    ((this + LEGACY_WIDGET_GUTTER_DP) / LEGACY_WIDGET_CELL_DP)
        .toInt()
        .coerceIn(MIN_STANDARD_WIDGET_SPAN, MAX_STANDARD_WIDGET_SPAN)

// Treat 70n - 30 dp as the lower bound for a span. Launchers can allocate more
// space per cell, so midpoint rounding can incorrectly promote a wide 4-cell
// widget to 5 cells (Pixel Launcher reports 293 dp for that 4-cell width).
private const val LEGACY_WIDGET_CELL_DP = 70f
private const val LEGACY_WIDGET_GUTTER_DP = 30f
private const val MIN_STANDARD_WIDGET_SPAN = 2
private const val MAX_STANDARD_WIDGET_SPAN = 5
