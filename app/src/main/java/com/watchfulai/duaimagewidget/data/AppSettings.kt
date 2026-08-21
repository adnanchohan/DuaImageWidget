package com.watchfulai.duaimagewidget.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    URDU,
    ARABIC,
}

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val defaultCropMode: CropMode = CropMode.FIT,
    val autoSaveCropByDefault: Boolean = false,
    val defaultWidgetBackground: Int = DEFAULT_WIDGET_BACKGROUND,
)

class AppSettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    val settings: Flow<AppSettings> = appContext.appSettingsDataStore.data.map { preferences ->
        AppSettings(
            theme = preferences[Keys.theme]
                ?.let { stored -> AppTheme.entries.firstOrNull { it.name == stored } }
                ?: AppTheme.SYSTEM,
            language = preferences[Keys.language]
                ?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }
                ?: AppLanguage.SYSTEM,
            defaultCropMode = preferences[Keys.defaultCropMode]
                ?.let { stored -> CropMode.entries.firstOrNull { it.name == stored } }
                ?: CropMode.FIT,
            autoSaveCropByDefault = preferences[Keys.autoSaveCropByDefault] ?: false,
            defaultWidgetBackground = preferences[Keys.defaultWidgetBackground]
                ?: DEFAULT_WIDGET_BACKGROUND,
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        appContext.appSettingsDataStore.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        appContext.appSettingsDataStore.edit { it[Keys.language] = language.name }
    }

    suspend fun setDefaultCropMode(mode: CropMode) {
        appContext.appSettingsDataStore.edit { it[Keys.defaultCropMode] = mode.name }
    }

    suspend fun setAutoSaveCropByDefault(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { it[Keys.autoSaveCropByDefault] = enabled }
    }

    suspend fun setDefaultWidgetBackground(color: Int) {
        appContext.appSettingsDataStore.edit { it[Keys.defaultWidgetBackground] = color }
    }

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val defaultCropMode = stringPreferencesKey("default_crop_mode")
        val autoSaveCropByDefault = booleanPreferencesKey("auto_save_crop_by_default")
        val defaultWidgetBackground = intPreferencesKey("default_widget_background")
    }
}
