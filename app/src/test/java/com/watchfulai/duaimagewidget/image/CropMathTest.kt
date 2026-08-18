package com.watchfulai.duaimagewidget.image

import com.watchfulai.duaimagewidget.data.CropMode
import com.watchfulai.duaimagewidget.data.CropTransform
import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {
    @Test
    fun fit_keepsTheWholeImageVisible() {
        val geometry = CropMath.geometry(
            sourceWidth = 400,
            sourceHeight = 200,
            targetWidth = 200,
            targetHeight = 200,
            mode = CropMode.FIT,
            transform = CropTransform(),
        )

        assertEquals(0f, geometry.left, 0.001f)
        assertEquals(50f, geometry.top, 0.001f)
        assertEquals(200f, geometry.width, 0.001f)
        assertEquals(100f, geometry.height, 0.001f)
    }

    @Test
    fun fill_coversTheFrameWithoutEmptyEdges() {
        val geometry = CropMath.geometry(
            sourceWidth = 400,
            sourceHeight = 200,
            targetWidth = 200,
            targetHeight = 200,
            mode = CropMode.FILL,
            transform = CropTransform(focalX = 0f, focalY = 0f),
        )

        assertEquals(0f, geometry.left, 0.001f)
        assertEquals(0f, geometry.top, 0.001f)
        assertEquals(400f, geometry.width, 0.001f)
        assertEquals(200f, geometry.height, 0.001f)
    }

    @Test
    fun pan_movesTheFocalPointInTheOppositeDirection() {
        val transformed = CropMath.transformed(
            current = CropTransform(),
            panX = 50f,
            panY = 0f,
            zoomChange = 1f,
            sourceWidth = 400,
            sourceHeight = 200,
            targetWidth = 200,
            targetHeight = 200,
        )

        assertEquals(0.375f, transformed.focalX, 0.001f)
        assertEquals(0.5f, transformed.focalY, 0.001f)
    }

    @Test
    fun zoom_isClampedToSupportedRange() {
        val transformed = CropMath.transformed(
            current = CropTransform(),
            panX = 0f,
            panY = 0f,
            zoomChange = 20f,
            sourceWidth = 400,
            sourceHeight = 200,
            targetWidth = 200,
            targetHeight = 200,
        )

        assertEquals(CropMath.MAX_ZOOM, transformed.zoom, 0.001f)
    }
}
