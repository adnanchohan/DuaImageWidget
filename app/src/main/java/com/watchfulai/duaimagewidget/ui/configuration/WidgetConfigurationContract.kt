package com.watchfulai.duaimagewidget.ui.configuration

import android.content.Intent

internal data class WidgetSizeDp(
    val width: Float,
    val height: Float,
) {
    val isValid: Boolean
        get() = width.isFinite() && height.isFinite() && width > 0f && height > 0f
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
