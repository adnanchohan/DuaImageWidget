package com.watchfulai.duaimagewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchfulai.duaimagewidget.data.ActiveWidgetsTracker
import com.watchfulai.duaimagewidget.data.AppSettings
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.ui.components.BrandMark
import com.watchfulai.duaimagewidget.ui.components.DuaIconButton
import com.watchfulai.duaimagewidget.ui.components.DuaPill
import com.watchfulai.duaimagewidget.ui.components.DuaSurfaceCard
import com.watchfulai.duaimagewidget.ui.LocaleAwareActivity
import com.watchfulai.duaimagewidget.ui.settings.SettingsActivity
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import com.watchfulai.duaimagewidget.ui.theme.Gold300
import com.watchfulai.duaimagewidget.ui.theme.Gold500
import com.watchfulai.duaimagewidget.ui.widgets.WidgetGalleryActivity
import com.watchfulai.duaimagewidget.ui.widgets.YourWidgetsActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

class MainActivity : LocaleAwareActivity() {
    private val settingsRepository by lazy { AppSettingsRepository(applicationContext) }
    private var activeWidgetCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            DuaImageWidgetTheme(appTheme = settings.theme) {
                HomeScreen(
                    activeWidgetCount = activeWidgetCount,
                    onSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onYourWidgets = {
                        startActivity(Intent(this, YourWidgetsActivity::class.java))
                    },
                    onWidgetGallery = {
                        startActivity(Intent(this, WidgetGalleryActivity::class.java))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeWidgetCount = ActiveWidgetsTracker.getTotalActiveWidgetCount(this)
    }
}

@Composable
private fun HomeScreen(
    activeWidgetCount: Int,
    onSettings: () -> Unit,
    onYourWidgets: () -> Unit,
    onWidgetGallery: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DuaPill(text = stringResource(R.string.home_reminder_pill))
                    val heroPrefix = stringResource(R.string.home_hero_prefix)
                    val heroDua = stringResource(R.string.home_hero_dua)
                    val heroSuffix = stringResource(R.string.home_hero_suffix)
                    Text(
                        text = buildAnnotatedString {
                            append(heroPrefix)
                            withStyle(
                                SpanStyle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF8C6514),
                                            Color(0xFFFFE082),
                                            Color(0xFFD4AF37),
                                            Color(0xFFFFF1A8),
                                            Color(0xFF9B741A),
                                        ),
                                    ),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black,
                                ),
                            ) {
                                append(heroDua)
                            }
                            append(heroSuffix)
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.home_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DuaIconButton(
                    icon = R.drawable.ic_settings,
                    contentDescription = stringResource(R.string.open_settings),
                    onClick = onSettings,
                )
            }

            // "Your Widgets" tile is only shown when ANY widget is applied
            if (activeWidgetCount > 0) {
                YourWidgetsTile(
                    activeWidgetCount = activeWidgetCount,
                    onClick = onYourWidgets,
                )
            }

            // "Add a Widget" tile is always visible
            AddWidgetTile(
                onClick = onWidgetGallery,
            )

            WidgetShowcase()

            DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        stringResource(R.string.home_steps_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    HomeStep(
                        "01",
                        stringResource(R.string.home_step_add_title),
                        stringResource(R.string.home_step_add_description),
                    )
                    HomeStep(
                        "02",
                        stringResource(R.string.home_step_pick_title),
                        stringResource(R.string.home_step_pick_description),
                    )
                    HomeStep(
                        "03",
                        stringResource(R.string.home_step_frame_title),
                        stringResource(R.string.home_step_frame_description),
                    )
                }
            }
        }
    }
}

@Composable
private fun YourWidgetsTile(
    activeWidgetCount: Int,
    onClick: () -> Unit,
) {
    DuaSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    painter = painterResource(R.drawable.ic_widgets),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    stringResource(R.string.home_your_widgets_title),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.home_your_widgets_description),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.widgets_active_count,
                            activeWidgetCount,
                            activeWidgetCount,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.open_your_widgets),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AddWidgetTile(
    onClick: () -> Unit,
) {
    DuaSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    stringResource(R.string.home_add_widget_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.home_add_widget_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private enum class ShowcaseTab {
    PRAYER,
    DUA,
    TASBEEH,
}

@Composable
private fun WidgetShowcase() {
    var selectedTab by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(ShowcaseTab.PRAYER)
    }

    DuaSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Showcase Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ShowcaseTabButton(
                    label = stringResource(R.string.showcase_tab_prayer),
                    selected = selectedTab == ShowcaseTab.PRAYER,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = ShowcaseTab.PRAYER },
                )
                ShowcaseTabButton(
                    label = stringResource(R.string.showcase_tab_dua),
                    selected = selectedTab == ShowcaseTab.DUA,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = ShowcaseTab.DUA },
                )
                ShowcaseTabButton(
                    label = stringResource(R.string.showcase_tab_tasbeeh),
                    selected = selectedTab == ShowcaseTab.TASBEEH,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = ShowcaseTab.TASBEEH },
                )
            }

            // Showcase Preview Card with smooth animation
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "showcase_transition",
            ) { tab ->
                when (tab) {
                    ShowcaseTab.PRAYER -> PrayerShowcaseCard()
                    ShowcaseTab.DUA -> DuaShowcaseCard()
                    ShowcaseTab.TASBEEH -> TasbeehShowcaseCard()
                }
            }
        }
    }
}

@Composable
private fun ShowcaseTabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 9.dp),
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PrayerShowcaseCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mosque),
                        contentDescription = null,
                        tint = Gold300,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.showcase_prayer_city),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    stringResource(R.string.showcase_prayer_hijri),
                    color = Gold300,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.15f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            stringResource(R.string.showcase_prayer_next),
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Sunrise: 06:12",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Gold300,
                    ) {
                        Text(
                            "LIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color(0xFF3E2723),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val prayers = listOf(
                    "Fajr" to "05:15",
                    "Dhuhr" to "12:30",
                    "Asr" to "15:45",
                    "Maghrib" to "18:10",
                    "Isha" to "19:35",
                )
                prayers.forEach { (name, time) ->
                    val isCurrent = name == "Asr"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) Gold300 else Color.White.copy(alpha = 0.12f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = name,
                                color = if (isCurrent) Color(0xFF3E2723) else Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(
                                text = time,
                                color = if (isCurrent) Color(0xFF3E2723) else Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaShowcaseCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Gold300, CircleShape),
            )
            Text(
                text = stringResource(R.string.home_showcase_dua),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.home_showcase_translation),
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
            )
            DuaPill(
                text = stringResource(R.string.home_showcase_size_pill),
            )
        }
    }
}

@Composable
private fun TasbeehShowcaseCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_counter),
                        contentDescription = null,
                        tint = Gold300,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(R.string.gallery_tasbeeh_name),
                        color = Gold300,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.showcase_tasbeeh_dhikr),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.showcase_tasbeeh_meaning),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(2.dp, Gold300),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.showcase_tasbeeh_count),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.showcase_tasbeeh_tap),
                        color = Gold300,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStep(number: String, title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = number,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
