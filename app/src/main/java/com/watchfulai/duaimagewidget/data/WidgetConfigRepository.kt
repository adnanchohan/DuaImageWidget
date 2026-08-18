package com.watchfulai.duaimagewidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetConfigDataStore by preferencesDataStore(name = "widget_configurations")

class WidgetConfigRepository(context: Context) {
    private val appContext = context.applicationContext

    fun observe(appWidgetId: Int): Flow<WidgetConfig?> =
        appContext.widgetConfigDataStore.data.map { preferences ->
            preferences.toWidgetConfig(appWidgetId)
        }

    suspend fun get(appWidgetId: Int): WidgetConfig? = observe(appWidgetId).first()

    private fun androidx.datastore.preferences.core.Preferences.toWidgetConfig(
        appWidgetId: Int,
    ): WidgetConfig? {
        val imageFileName = this[Keys.image(appWidgetId)] ?: return null
        val mode = this[Keys.mode(appWidgetId)]
            ?.let { stored -> CropMode.entries.firstOrNull { it.name == stored } }
            ?: CropMode.FIT

        return WidgetConfig(
            appWidgetId = appWidgetId,
            imageFileName = imageFileName,
            cropMode = mode,
            cropTransform = CropTransform(
                focalX = this[Keys.focalX(appWidgetId)] ?: 0.5f,
                focalY = this[Keys.focalY(appWidgetId)] ?: 0.5f,
                zoom = this[Keys.zoom(appWidgetId)] ?: 1f,
            ),
            backgroundColor = this[Keys.background(appWidgetId)]
                ?: DEFAULT_WIDGET_BACKGROUND,
        )
    }

    suspend fun save(config: WidgetConfig) {
        appContext.widgetConfigDataStore.edit { preferences ->
            preferences[Keys.image(config.appWidgetId)] = config.imageFileName
            preferences[Keys.mode(config.appWidgetId)] = config.cropMode.name
            preferences[Keys.focalX(config.appWidgetId)] = config.cropTransform.focalX
            preferences[Keys.focalY(config.appWidgetId)] = config.cropTransform.focalY
            preferences[Keys.zoom(config.appWidgetId)] = config.cropTransform.zoom
            preferences[Keys.background(config.appWidgetId)] = config.backgroundColor
        }
    }

    suspend fun remove(appWidgetId: Int): WidgetConfig? {
        val existing = get(appWidgetId)
        appContext.widgetConfigDataStore.edit { preferences ->
            preferences.remove(Keys.image(appWidgetId))
            preferences.remove(Keys.mode(appWidgetId))
            preferences.remove(Keys.focalX(appWidgetId))
            preferences.remove(Keys.focalY(appWidgetId))
            preferences.remove(Keys.zoom(appWidgetId))
            preferences.remove(Keys.background(appWidgetId))
        }
        return existing
    }

    private object Keys {
        fun image(id: Int) = stringPreferencesKey("widget_${id}_image")
        fun mode(id: Int) = stringPreferencesKey("widget_${id}_mode")
        fun focalX(id: Int) = floatPreferencesKey("widget_${id}_focal_x")
        fun focalY(id: Int) = floatPreferencesKey("widget_${id}_focal_y")
        fun zoom(id: Int) = floatPreferencesKey("widget_${id}_zoom")
        fun background(id: Int) = intPreferencesKey("widget_${id}_background")
    }
}
