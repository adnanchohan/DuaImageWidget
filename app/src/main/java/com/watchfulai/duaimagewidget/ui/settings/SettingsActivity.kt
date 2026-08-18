package com.watchfulai.duaimagewidget.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watchfulai.duaimagewidget.R
import com.watchfulai.duaimagewidget.data.AppSettings
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.data.AppTheme
import com.watchfulai.duaimagewidget.data.CropMode
import com.watchfulai.duaimagewidget.data.DEFAULT_WIDGET_BACKGROUND
import com.watchfulai.duaimagewidget.ui.components.BrandMark
import com.watchfulai.duaimagewidget.ui.components.DuaIconButton
import com.watchfulai.duaimagewidget.ui.components.DuaSurfaceCard
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    private val repository by lazy { AppSettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by repository.settings.collectAsState(initial = AppSettings())
            val scope = rememberCoroutineScope()
            DuaImageWidgetTheme(appTheme = settings.theme) {
                SettingsScreen(
                    settings = settings,
                    onBack = ::finish,
                    onThemeChanged = { scope.launch { repository.setTheme(it) } },
                    onDefaultCropChanged = {
                        scope.launch { repository.setDefaultCropMode(it) }
                    },
                    onAutoSaveChanged = {
                        scope.launch { repository.setAutoSaveCropByDefault(it) }
                    },
                    onBackgroundChanged = {
                        scope.launch { repository.setDefaultWidgetBackground(it) }
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onDefaultCropChanged: (CropMode) -> Unit,
    onAutoSaveChanged: (Boolean) -> Unit,
    onBackgroundChanged: (Int) -> Unit,
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
                    contentDescription = "Back",
                    onClick = onBack,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Make every new widget feel like yours.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BrandMark(size = 40.dp)
            }

            SettingsSection(
                title = "Appearance",
                description = "Choose how the app looks on this device.",
            ) {
                SegmentedSelector {
                    AppTheme.entries.forEach { theme ->
                        SegmentedOption(
                            label = theme.displayName,
                            selected = settings.theme == theme,
                            modifier = Modifier.weight(1f),
                            onClick = { onThemeChanged(theme) },
                        )
                    }
                }
            }

            SettingsSection(
                title = "New widget defaults",
                description = "These choices are applied the first time you add a widget.",
            ) {
                Text("Image display", style = MaterialTheme.typography.titleSmall)
                SegmentedSelector {
                    SegmentedOption(
                        label = "Fit",
                        selected = settings.defaultCropMode == CropMode.FIT,
                        modifier = Modifier.weight(1f),
                        onClick = { onDefaultCropChanged(CropMode.FIT) },
                    )
                    SegmentedOption(
                        label = "Fill & crop",
                        selected = settings.defaultCropMode == CropMode.FILL,
                        modifier = Modifier.weight(1f),
                        onClick = { onDefaultCropChanged(CropMode.FILL) },
                    )
                }
                SettingSwitchRow(
                    title = "Auto-save crop",
                    description = "Keep future crop and position changes without tapping Save.",
                    checked = settings.autoSaveCropByDefault,
                    onCheckedChange = onAutoSaveChanged,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Fit background", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(
                            DEFAULT_WIDGET_BACKGROUND,
                            0xFFFFFFFF.toInt(),
                            0xFFE4F3EE.toInt(),
                            0xFF17201D.toInt(),
                        ).forEach { color ->
                            BackgroundSwatch(
                                color = color,
                                selected = settings.defaultWidgetBackground == color,
                                onClick = { onBackgroundChanged(color) },
                            )
                        }
                    }
                    Text(
                        "Visible around images that use Fit mode.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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
                        Text("Private on your device", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Dua images are never uploaded and the system photo picker keeps your library private.",
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
                    "Dua Image Widget",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    "Version 1.0",
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
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BackgroundSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ringColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .size(46.dp)
            .border(BorderStroke(if (selected) 3.dp else 1.dp, ringColor), CircleShape)
            .padding(5.dp)
            .background(Color(color), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(
                        if (color == 0xFF17201D.toInt()) Color.White else MaterialTheme.colorScheme.primary,
                        CircleShape,
                    ),
            )
        }
    }
}

private val AppTheme.displayName: String
    get() = when (this) {
        AppTheme.SYSTEM -> "System"
        AppTheme.LIGHT -> "Light"
        AppTheme.DARK -> "Dark"
    }
