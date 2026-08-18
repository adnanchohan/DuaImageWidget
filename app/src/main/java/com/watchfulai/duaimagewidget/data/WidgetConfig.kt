package com.watchfulai.duaimagewidget.data

enum class CropMode {
    FIT,
    FILL,
}

data class CropTransform(
    val focalX: Float = 0.5f,
    val focalY: Float = 0.5f,
    val zoom: Float = 1f,
)

data class WidgetConfig(
    val appWidgetId: Int,
    val imageFileName: String,
    val cropMode: CropMode = CropMode.FIT,
    val cropTransform: CropTransform = CropTransform(),
    val backgroundColor: Int = DEFAULT_WIDGET_BACKGROUND,
)

const val DEFAULT_WIDGET_BACKGROUND: Int = 0xFFF7F2E8.toInt()
