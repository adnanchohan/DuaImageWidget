package com.watchfulai.duaimagewidget.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.watchfulai.duaimagewidget.data.WidgetConfig
import kotlin.math.roundToInt
import kotlin.math.sqrt

object WidgetBitmapRenderer {
    private const val MAX_OUTPUT_EDGE = 1_600
    private const val MAX_OUTPUT_PIXELS = 900_000

    fun render(
        source: Bitmap,
        requestedWidth: Int,
        requestedHeight: Int,
        config: WidgetConfig,
    ): Bitmap {
        val (targetWidth, targetHeight) = constrainedSize(requestedWidth, requestedHeight)
        val output = createBitmap(targetWidth, targetHeight)
        val canvas = Canvas(output)
        canvas.drawColor(config.backgroundColor)

        val geometry = CropMath.geometry(
            sourceWidth = source.width,
            sourceHeight = source.height,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            mode = config.cropMode,
            transform = config.cropTransform,
        )
        val destination = RectF(
            geometry.left,
            geometry.top,
            geometry.left + geometry.width,
            geometry.top + geometry.height,
        )
        canvas.drawBitmap(
            source,
            null,
            destination,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG),
        )
        return output
    }

    private fun constrainedSize(width: Int, height: Int): Pair<Int, Int> {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val edgeScale = minOf(
            1f,
            MAX_OUTPUT_EDGE.toFloat() / safeWidth,
            MAX_OUTPUT_EDGE.toFloat() / safeHeight,
        )
        val edgeWidth = (safeWidth * edgeScale).roundToInt().coerceAtLeast(1)
        val edgeHeight = (safeHeight * edgeScale).roundToInt().coerceAtLeast(1)
        val pixelScale = if (edgeWidth.toLong() * edgeHeight <= MAX_OUTPUT_PIXELS) {
            1f
        } else {
            sqrt(MAX_OUTPUT_PIXELS.toFloat() / (edgeWidth.toFloat() * edgeHeight))
        }
        return Pair(
            (edgeWidth * pixelScale).roundToInt().coerceAtLeast(1),
            (edgeHeight * pixelScale).roundToInt().coerceAtLeast(1),
        )
    }
}
