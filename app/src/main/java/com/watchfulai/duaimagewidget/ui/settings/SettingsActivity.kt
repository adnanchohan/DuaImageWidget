package com.watchfulai.duaimagewidget.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import androidx.core.os.LocaleListCompat
import com.watchfulai.duaimagewidget.R
import com.watchfulai.duaimagewidget.data.AppLanguage
import com.watchfulai.duaimagewidget.data.AppSettings
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.data.AppTheme
import com.watchfulai.duaimagewidget.ui.components.BrandMark
import com.watchfulai.duaimagewidget.ui.components.DuaIconButton
import com.watchfulai.duaimagewidget.ui.components.DuaSurfaceCard
import com.watchfulai.duaimagewidget.ui.LocaleAwareActivity
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import com.watchfulai.duaimagewidget.widget.DuaImageWidgetReceiver
import kotlinx.coroutines.launch

class SettingsActivity : LocaleAwareActivity() {
    private val repository by lazy { AppSettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by repository.settings.collectAsState(initial = AppSettings())
            val scope = rememberCoroutineScope()
            val selectedLanguage = AppLanguage.fromLanguageTag(
                AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag(),
            )
            DuaImageWidgetTheme(appTheme = settings.theme) {
                SettingsScreen(
                    settings = settings,
                    selectedLanguage = selectedLanguage,
                    onBack = ::finish,
                    onThemeChanged = { scope.launch { repository.setTheme(it) } },
                    onMoreApps = ::openMoreApps,
                    onShareApp = ::shareApp,
                    onLanguageChanged = { language ->
                        scope.launch {
                            repository.setLanguage(language)
                            AppCompatDelegate.setApplicationLocales(language.toLocaleList())
                            requestWidgetLocaleRefresh()
                        }
                    },
                )
            }
        }
    }

    private fun requestWidgetLocaleRefresh() {
        val receiver = ComponentName(applicationContext, DuaImageWidgetReceiver::class.java)
        val widgetIds = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(receiver)
        if (widgetIds.isEmpty()) return

        applicationContext.sendBroadcast(
            Intent(applicationContext, DuaImageWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            },
        )
    }

    private fun openMoreApps() {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://search?q=pub:WatchFulAI"),
        )
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/search?q=WatchFulAI&c=apps"),
        )
        runCatching { startActivity(marketIntent) }
            .onFailure { startActivity(webIntent) }
    }

    private fun shareApp() {
        val storeUrl = "https://play.google.com/store/apps/details?id=$packageName"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.share_message, storeUrl),
            )
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)))
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    selectedLanguage: AppLanguage,
    onBack: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onMoreApps: () -> Unit,
    onShareApp: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DuaIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.settings_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BrandMark(size = 40.dp)
            }

            SettingsSection(
                title = stringResource(R.string.settings_appearance_title),
                description = stringResource(R.string.settings_appearance_description),
            ) {
                SegmentedSelector {
                    AppTheme.entries.forEach { theme ->
                        SegmentedOption(
                            label = theme.displayName(),
                            selected = settings.theme == theme,
                            modifier = Modifier.weight(1f),
                            onClick = { onThemeChanged(theme) },
                        )
                    }
                }
            }

            SettingsSection(
                title = stringResource(R.string.settings_more_title),
                description = stringResource(R.string.settings_more_description),
            ) {
                SettingsActionRow(
                    icon = R.drawable.ic_apps,
                    title = stringResource(R.string.settings_more_apps_title),
                    description = stringResource(R.string.settings_more_apps_description),
                    onClick = onMoreApps,
                )
                SettingsActionRow(
                    icon = R.drawable.ic_share,
                    title = stringResource(R.string.settings_share_title),
                    description = stringResource(R.string.settings_share_description),
                    onClick = onShareApp,
                )
                LanguageSettingRow(
                    selected = selectedLanguage,
                    onSelected = onLanguageChanged,
                )
            }

            DuaSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shield),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(11.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_private_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.settings_private_description),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.app_name),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    stringResource(R.string.settings_version),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            content()
        }
    }
}

@Composable
private fun SegmentedSelector(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun SegmentedOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SettingsActionRow(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(10.dp)
                    .size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LanguageSettingRow(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        SettingsActionRow(
            icon = R.drawable.ic_language,
            title = stringResource(R.string.settings_language_title),
            description = stringResource(R.string.settings_language_description),
            onClick = { expanded = true },
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        selected.displayName(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            language.displayName(),
                            fontWeight = if (language == selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(language)
                    },
                    trailingIcon = if (language == selected) {
                        {
                            Text(
                                "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun AppTheme.displayName(): String = when (this) {
        AppTheme.SYSTEM -> stringResource(R.string.theme_system)
        AppTheme.LIGHT -> stringResource(R.string.theme_light)
        AppTheme.DARK -> stringResource(R.string.theme_dark)
    }

@Composable
private fun AppLanguage.displayName(): String = when (this) {
        AppLanguage.SYSTEM -> stringResource(R.string.language_system_default)
        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
        AppLanguage.URDU -> stringResource(R.string.language_urdu)
        AppLanguage.ARABIC -> stringResource(R.string.language_arabic)
    }

private fun AppLanguage.toLocaleList(): LocaleListCompat =
    if (this == AppLanguage.SYSTEM) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageTag)
    }
