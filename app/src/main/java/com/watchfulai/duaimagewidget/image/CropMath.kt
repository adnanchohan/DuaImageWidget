package com.watchfulai.duaimagewidget.image

import com.watchfulai.duaimagewidget.data.CropMode
import com.watchfulai.duaimagewidget.data.CropTransform
import kotlin.math.max
import kotlin.math.min

data class DrawGeometry(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

object CropMath {
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 6f

    fun geometry(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        mode: CropMode,
        transform: CropTransform,
    ): DrawGeometry {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return DrawGeometry(0f, 0f, 0f, 0f)
        }

        val widthScale = targetWidth.toFloat() / sourceWidth
        val heightScale = targetHeight.toFloat() / sourceHeight

        if (mode == CropMode.FIT) {
            val scale = min(widthScale, heightScale)
            val width = sourceWidth * scale
            val height = sourceHeight * scale
            return DrawGeometry(
                left = (targetWidth - width) / 2f,
                top = (targetHeight - height) / 2f,
                width = width,
                height = height,
            )
        }

        val zoom = transform.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val scale = max(widthScale, heightScale) * zoom
        val width = sourceWidth * scale
        val height = sourceHeight * scale
        val focalX = clampFocal(transform.focalX, targetWidth / (2f * width))
        val focalY = clampFocal(transform.focalY, targetHeight / (2f * height))

        return DrawGeometry(
            left = targetWidth / 2f - focalX * width,
            top = targetHeight / 2f - focalY * height,
            width = width,
            height = height,
        )
    }

    fun transformed(
        current: CropTransform,
        panX: Float,
        panY: Float,
        zoomChange: Float,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): CropTransform {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return current
        }

        val newZoom = (current.zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val baseScale = max(
            targetWidth.toFloat() / sourceWidth,
            targetHeight.toFloat() / sourceHeight,
        )
        val scaledWidth = sourceWidth * baseScale * newZoom
        val scaledHeight = sourceHeight * baseScale * newZoom
        val focalX = current.focalX - panX / scaledWidth
        val focalY = current.focalY - panY / scaledHeight

        return CropTransform(
            focalX = clampFocal(focalX, targetWidth / (2f * scaledWidth)),
            focalY = clampFocal(focalY, targetHeight / (2f * scaledHeight)),
            zoom = newZoom,
        )
    }

    fun normalized(
        transform: CropTransform,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): CropTransform = transformed(
        current = transform,
        panX = 0f,
        panY = 0f,
        zoomChange = 1f,
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        targetWidth = targetWidth,
        targetHeight = targetHeight,
    )

    private fun clampFocal(value: Float, halfVisibleFraction: Float): Float {
        val half = halfVisibleFraction.coerceIn(0f, 0.5f)
        return value.coerceIn(half, 1f - half)
    }
}
