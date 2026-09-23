package com.watchfulai.mywidgets.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.watchfulai.mywidgets.data.AppLanguage
import com.watchfulai.mywidgets.data.AppSettingsRepository
import kotlinx.coroutines.launch

abstract class LocaleAwareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val storedLanguage = AppSettingsRepository(applicationContext)
                .takeLanguageForAppCompatMigration()
            if (
                storedLanguage != null &&
                storedLanguage != AppLanguage.SYSTEM &&
                AppCompatDelegate.getApplicationLocales().isEmpty
            ) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(storedLanguage.languageTag),
                )
            }
        }
    }
}
