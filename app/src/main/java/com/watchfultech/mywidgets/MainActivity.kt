package com.watchfultech.mywidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.watchfultech.mywidgets.data.ActiveWidgetsTracker
import com.watchfultech.mywidgets.data.AppSettings
import com.watchfultech.mywidgets.data.AppSettingsRepository
import com.watchfultech.mywidgets.ui.components.BrandMark
import com.watchfultech.mywidgets.ui.components.DuaIconButton
import com.watchfultech.mywidgets.ui.components.DuaPill
import com.watchfultech.mywidgets.ui.components.DuaSurfaceCard
import com.watchfultech.mywidgets.ui.LocaleAwareActivity
import com.watchfultech.mywidgets.ui.settings.SettingsActivity
import com.watchfultech.mywidgets.ui.theme.DuaImageWidgetTheme
import com.watchfultech.mywidgets.ui.theme.Gold300
import com.watchfultech.mywidgets.ui.theme.Gold500
import com.watchfultech.mywidgets.ui.theme.LocalWidgetColors
import com.watchfultech.mywidgets.ui.widgets.WidgetGalleryActivity
import com.watchfultech.mywidgets.ui.widgets.YourWidgetsActivity
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

            IslamicWidgetShowcase()

            GeneralWidgetShowcase()

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

private enum class IslamicShowcaseTab {
    PRAYER,
    DUA,
    TASBEEH,
}

private enum class GeneralShowcaseTab {
    IMAGE,
    NOTES,
}

@Composable
private fun IslamicWidgetShowcase() {
    var selectedTab by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(IslamicShowcaseTab.PRAYER)
    }

    DuaSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.showcase_category_islamic),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

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
                    selected = selectedTab == IslamicShowcaseTab.PRAYER,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = IslamicShowcaseTab.PRAYER },
                )
                ShowcaseTabButton(
                    label = stringResource(R.string.showcase_tab_dua),
                    selected = selectedTab == IslamicShowcaseTab.DUA,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = IslamicShowcaseTab.DUA },
                )
                ShowcaseTabButton(
                    label = stringResource(R.string.showcase_tab_tasbeeh),
                    selected = selectedTab == IslamicShowcaseTab.TASBEEH,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = IslamicShowcaseTab.TASBEEH },
                )
            }

            // Showcase Preview Card with smooth animation
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "islamic_showcase_transition",
            ) { tab ->
                when (tab) {
                    IslamicShowcaseTab.PRAYER -> PrayerShowcaseCard()
                    IslamicShowcaseTab.DUA -> DuaShowcaseCard()
                    IslamicShowcaseTab.TASBEEH -> TasbeehShowcaseCard()
                }
            }
        }
    }
}

@Composable
private fun GeneralWidgetShowcase() {
    var selectedTab by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(GeneralShowcaseTab.IMAGE)
    }

    DuaSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.showcase_category_general),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

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
                    label = stringResource(R.string.showcase_tab_image),
                    selected = selectedTab == GeneralShowcaseTab.IMAGE,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = GeneralShowcaseTab.IMAGE },
                )
                ShowcaseTabButton(
                    label = stringResource(R.string.showcase_tab_notes),
                    selected = selectedTab == GeneralShowcaseTab.NOTES,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = GeneralShowcaseTab.NOTES },
                )
            }

            // Showcase Preview Card with smooth animation
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "general_showcase_transition",
            ) { tab ->
                when (tab) {
                    GeneralShowcaseTab.IMAGE -> ImageShowcaseCard()
                    GeneralShowcaseTab.NOTES -> NotesShowcaseCard()
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
    val widgetColors = LocalWidgetColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        widgetColors.backgroundStart,
                        widgetColors.backgroundEnd,
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Row: Mosque Icon & Hijri Date (Location shifted to Next Prayer tile)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mosque),
                    contentDescription = null,
                    tint = widgetColors.gold,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.showcase_prayer_hijri),
                    color = widgetColors.gold,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Hero Card: Next prayer & (Location • Sunrise) without LIVE badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = widgetColors.heroBg,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            stringResource(R.string.showcase_prayer_next),
                            color = widgetColors.text,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.showcase_prayer_city),
                                color = widgetColors.secondaryText,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                "•",
                                color = widgetColors.secondaryText,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                "Sunrise: 06:12",
                                color = widgetColors.secondaryText,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            // Bottom Slots: 5 prayer times with small am/pm and full names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val prayers = listOf(
                    Triple("Fajr", "05:15", "am"),
                    Triple("Dhuhr", "12:30", "pm"),
                    Triple("Asr", "03:45", "pm"),
                    Triple("Maghrib", "06:10", "pm"),
                    Triple("Isha", "07:35", "pm"),
                )
                prayers.forEach { (name, time, amPm) ->
                    val isCurrent = name == "Asr"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) widgetColors.accent else widgetColors.slot,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = name,
                                color = if (isCurrent) widgetColors.accentText else widgetColors.secondaryText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = time,
                                    color = if (isCurrent) widgetColors.accentText else widgetColors.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = amPm,
                                    color = if (isCurrent) widgetColors.accentText else widgetColors.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 0.5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaShowcaseCard() {
    val widgetColors = LocalWidgetColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        widgetColors.backgroundStart,
                        widgetColors.backgroundEnd,
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
                    .background(widgetColors.gold, CircleShape),
            )
            Text(
                text = stringResource(R.string.home_showcase_dua),
                textAlign = TextAlign.Center,
                color = widgetColors.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.home_showcase_translation),
                textAlign = TextAlign.Center,
                color = widgetColors.secondaryText,
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
    val widgetColors = LocalWidgetColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        widgetColors.backgroundStart,
                        widgetColors.backgroundEnd,
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
                        tint = widgetColors.gold,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(R.string.gallery_tasbeeh_name),
                        color = widgetColors.gold,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.showcase_tasbeeh_dhikr),
                    color = widgetColors.text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.showcase_tasbeeh_meaning),
                    color = widgetColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Surface(
                shape = CircleShape,
                color = widgetColors.heroBg,
                border = androidx.compose.foundation.BorderStroke(2.dp, widgetColors.gold),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.showcase_tasbeeh_count),
                        color = widgetColors.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.showcase_tasbeeh_tap),
                        color = widgetColors.gold,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageShowcaseCard() {
    val widgetColors = LocalWidgetColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        widgetColors.backgroundStart,
                        widgetColors.backgroundEnd,
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
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
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        tint = widgetColors.gold,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.showcase_image_title),
                        color = widgetColors.text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = widgetColors.accent,
                ) {
                    Text(
                        stringResource(R.string.showcase_image_badge),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = widgetColors.accentText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            // Hero Image Preview Frame
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = widgetColors.heroBg,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (widgetColors.isDark) {
                                        listOf(Color(0xFF143B33), Color(0xFF0D2822))
                                    } else {
                                        listOf(Color(0xFF236553), Color(0xFF17473A))
                                    },
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .border(
                                width = 1.dp,
                                color = widgetColors.gold.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_crop),
                                contentDescription = null,
                                tint = widgetColors.gold,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                stringResource(R.string.showcase_image_caption),
                                color = widgetColors.text,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }
                }
            }

            // Feature Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(
                    stringResource(R.string.showcase_image_feature_crop),
                    stringResource(R.string.showcase_image_feature_fit),
                    stringResource(R.string.showcase_image_feature_private),
                ).forEach { feature ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = widgetColors.slot,
                    ) {
                        Text(
                            text = feature,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = widgetColors.text,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesShowcaseCard() {
    val widgetColors = LocalWidgetColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        widgetColors.backgroundStart,
                        widgetColors.backgroundEnd,
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
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
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null,
                        tint = widgetColors.gold,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.showcase_notes_title),
                        color = widgetColors.text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = widgetColors.accent,
                ) {
                    Text(
                        stringResource(R.string.showcase_notes_badge),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = widgetColors.accentText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            // Note Content Frame
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = widgetColors.heroBg,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.showcase_notes_heading),
                            color = widgetColors.gold,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            shape = CircleShape,
                            color = widgetColors.slot,
                        ) {
                            Text(
                                "Today",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = widgetColors.text,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.showcase_notes_content),
                        color = widgetColors.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            // Feature Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(
                    stringResource(R.string.showcase_notes_feature_edit),
                    stringResource(R.string.showcase_notes_feature_style),
                    stringResource(R.string.showcase_notes_feature_lock),
                ).forEach { feature ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = widgetColors.slot,
                    ) {
                        Text(
                            text = feature,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = widgetColors.text,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
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
