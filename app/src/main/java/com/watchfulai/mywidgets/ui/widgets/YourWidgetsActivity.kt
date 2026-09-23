package com.watchfulai.mywidgets.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.watchfulai.mywidgets.R
import com.watchfulai.mywidgets.data.AppSettings
import com.watchfulai.mywidgets.data.AppSettingsRepository
import com.watchfulai.mywidgets.data.CropMode
import com.watchfulai.mywidgets.data.WidgetConfigRepository
import com.watchfulai.mywidgets.image.ImageStorage
import com.watchfulai.mywidgets.image.WidgetBitmapRenderer
import com.watchfulai.mywidgets.ui.LocaleAwareActivity
import com.watchfulai.mywidgets.ui.components.BrandMark
import com.watchfulai.mywidgets.ui.components.DuaIconButton
import com.watchfulai.mywidgets.ui.components.DuaPrimaryButton
import com.watchfulai.mywidgets.ui.components.DuaSurfaceCard
import com.watchfulai.mywidgets.ui.configuration.EXTRA_EDIT_FROM_WIDGET_LIST
import com.watchfulai.mywidgets.ui.configuration.WidgetConfigurationActivity
import com.watchfulai.mywidgets.ui.configuration.WidgetSizeDp
import com.watchfulai.mywidgets.ui.configuration.resolveWidgetSize
import com.watchfulai.mywidgets.ui.configuration.toWidgetCellSize
import com.watchfulai.mywidgets.ui.theme.DuaImageWidgetTheme
import com.watchfulai.mywidgets.prayer.PrayerSchedule
import com.watchfulai.mywidgets.prayer.PrayerTimesWidgetProvider
import com.watchfulai.mywidgets.prayer.PrayerWidgetRepository
import com.watchfulai.mywidgets.ui.prayer.PrayerWidgetConfigurationActivity
import com.watchfulai.mywidgets.ui.theme.Gold300
import com.watchfulai.mywidgets.widget.DuaImageWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class YourWidgetsActivity : LocaleAwareActivity() {
    private val settingsRepository by lazy { AppSettingsRepository(applicationContext) }
    private val widgetRepository by lazy { WidgetConfigRepository(applicationContext) }
    private val prayerRepository by lazy { PrayerWidgetRepository(applicationContext) }
    private var screenState by mutableStateOf(YourWidgetsState())
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            DuaImageWidgetTheme(appTheme = settings.theme) {
                YourWidgetsScreen(
                    state = screenState,
                    onBack = ::finish,
                    onEditWidget = ::editWidget,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshWidgets()
    }

    private fun refreshWidgets() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            screenState = screenState.copy(isLoading = true)
            val items = withContext(Dispatchers.Default) { loadActiveWidgets() }
            screenState = YourWidgetsState(isLoading = false, items = items)
        }
    }

    private suspend fun loadActiveWidgets(): List<ActiveWidgetSummary> {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val isLandscape = resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE

        val duaIds = manager.getAppWidgetIds(
            ComponentName(applicationContext, DuaImageWidgetReceiver::class.java),
        )
        val prayerIds = manager.getAppWidgetIds(
            ComponentName(applicationContext, PrayerTimesWidgetProvider::class.java),
        )

        val result = mutableListOf<ActiveWidgetSummary>()
        var displayCounter = 1

        duaIds.sorted().forEach { appWidgetId ->
            val options = manager.getAppWidgetOptions(appWidgetId)
            val size = resolveWidgetSize(
                exactSize = null,
                minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
                isLandscape = isLandscape,
            )
            val config = widgetRepository.get(appWidgetId)
            val preview = config?.let { savedConfig ->
                val (previewWidth, previewHeight) = previewDimensions(size)
                renderPreview(savedConfig.imageFileName) { source ->
                    WidgetBitmapRenderer.render(
                        source = source,
                        requestedWidth = previewWidth,
                        requestedHeight = previewHeight,
                        config = savedConfig,
                        cornerRadiusPx = previewCornerRadiusPx(),
                    )
                }
            }
            result.add(
                ActiveWidgetSummary.DuaImage(
                    appWidgetId = appWidgetId,
                    displayNumber = displayCounter++,
                    size = size,
                    cropMode = config?.cropMode,
                    preview = preview,
                ),
            )
        }

        prayerIds.sorted().forEach { appWidgetId ->
            val options = manager.getAppWidgetOptions(appWidgetId)
            val size = resolveWidgetSize(
                exactSize = null,
                minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
                isLandscape = isLandscape,
            )
            val config = prayerRepository.getConfig(appWidgetId)
            val schedule = prayerRepository.getSchedule(appWidgetId)
            result.add(
                ActiveWidgetSummary.PrayerTimes(
                    appWidgetId = appWidgetId,
                    displayNumber = displayCounter++,
                    size = size,
                    locationLabel = config?.locationLabel,
                    schedule = schedule,
                ),
            )
        }

        manager.getAppWidgetIds(ComponentName(applicationContext,
            com.watchfulai.mywidgets.tasbeeh.TasbeehWidgetProvider::class.java)).sorted().forEach { id ->
            result.add(ActiveWidgetSummary.Tasbeeh(id, displayCounter++, WidgetSizeDp(250f, 150f),
                com.watchfulai.mywidgets.tasbeeh.TasbeehRepository(applicationContext).get(id)))
        }
        return result
    }

    private suspend fun renderPreview(
        imageFileName: String,
        renderer: (Bitmap) -> Bitmap,
    ): Bitmap? {
        val source = runCatching {
            ImageStorage.load(applicationContext, imageFileName)
        }.getOrNull() ?: return null
        return try {
            renderer(source)
        } finally {
            source.recycle()
        }
    }

    private fun previewDimensions(size: WidgetSizeDp): Pair<Int, Int> {
        val ratio = (size.width / size.height).coerceAtLeast(0.1f)
        return if (ratio >= 1f) {
            PREVIEW_LONG_EDGE_PX to (PREVIEW_LONG_EDGE_PX / ratio).roundToInt().coerceAtLeast(1)
        } else {
            (PREVIEW_LONG_EDGE_PX * ratio).roundToInt().coerceAtLeast(1) to PREVIEW_LONG_EDGE_PX
        }
    }

    private fun previewCornerRadiusPx(): Float =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            resources.getDimension(android.R.dimen.system_app_widget_background_radius)
        } else {
            FALLBACK_CORNER_RADIUS_DP * resources.displayMetrics.density
        }

    private fun editWidget(item: ActiveWidgetSummary) {
        when (item) {
            is ActiveWidgetSummary.Tasbeeh -> startActivity(
                Intent(this, com.watchfulai.mywidgets.ui.tasbeeh.TasbeehConfigurationActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, item.appWidgetId))
            is ActiveWidgetSummary.DuaImage -> {
                startActivity(
                    Intent(this, WidgetConfigurationActivity::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, item.appWidgetId)
                        putExtra(EXTRA_EDIT_FROM_WIDGET_LIST, true)
                    },
                )
            }
            is ActiveWidgetSummary.PrayerTimes -> {
                startActivity(
                    Intent(this, PrayerWidgetConfigurationActivity::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, item.appWidgetId)
                    },
                )
            }
        }
    }

    private companion object {
        const val PREVIEW_LONG_EDGE_PX = 720
        const val FALLBACK_CORNER_RADIUS_DP = 20f
    }
}

private sealed interface ActiveWidgetSummary {
    val appWidgetId: Int
    val displayNumber: Int
    val size: WidgetSizeDp

    data class Tasbeeh(
        override val appWidgetId: Int,
        override val displayNumber: Int,
        override val size: WidgetSizeDp,
        val config: com.watchfulai.mywidgets.tasbeeh.TasbeehConfig?,
    ) : ActiveWidgetSummary

    data class DuaImage(
        override val appWidgetId: Int,
        override val displayNumber: Int,
        override val size: WidgetSizeDp,
        val cropMode: CropMode?,
        val preview: Bitmap?,
    ) : ActiveWidgetSummary

    data class PrayerTimes(
        override val appWidgetId: Int,
        override val displayNumber: Int,
        override val size: WidgetSizeDp,
        val locationLabel: String?,
        val schedule: PrayerSchedule?,
    ) : ActiveWidgetSummary
}

private data class YourWidgetsState(
    val isLoading: Boolean = true,
    val items: List<ActiveWidgetSummary> = emptyList(),
)

@Composable
private fun YourWidgetsScreen(
    state: YourWidgetsState,
    onBack: () -> Unit,
    onEditWidget: (ActiveWidgetSummary) -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
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
                        stringResource(R.string.widgets_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.widgets_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BrandMark(size = 40.dp)
            }

            when {
                state.isLoading -> WidgetsLoadingPanel()
                state.items.isEmpty() -> WidgetsEmptyPanel(onBack = onBack)
                else -> {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.widgets_active_count,
                                state.items.size,
                                state.items.size,
                            ),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    state.items.forEach { widget ->
                        when (widget) {
                            is ActiveWidgetSummary.DuaImage -> {
                                DuaWidgetSummaryCard(
                                    widget = widget,
                                    onClick = { onEditWidget(widget) },
                                )
                            }
                            is ActiveWidgetSummary.Tasbeeh -> {
                                DuaSurfaceCard(modifier = Modifier.fillMaxWidth().clickable { onEditWidget(widget) }) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(stringResource(R.string.gallery_tasbeeh_name), style = MaterialTheme.typography.titleMedium)
                                        com.watchfulai.mywidgets.ui.tasbeeh.TasbeehPreview(widget.config ?: com.watchfulai.mywidgets.tasbeeh.TasbeehConfig())
                                        Text(stringResource(R.string.tasbeeh_edit_reset), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            is ActiveWidgetSummary.PrayerTimes -> {
                                PrayerWidgetSummaryCard(
                                    widget = widget,
                                    onClick = { onEditWidget(widget) },
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
private fun WidgetsLoadingPanel() {
    DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.widgets_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WidgetsEmptyPanel(onBack: () -> Unit) {
    DuaSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 38.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    painter = painterResource(R.drawable.ic_widgets),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(30.dp),
                )
            }
            Text(
                stringResource(R.string.widgets_empty_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.widgets_empty_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            DuaPrimaryButton(
                text = stringResource(R.string.widgets_back_to_home),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DuaWidgetSummaryCard(
    widget: ActiveWidgetSummary.DuaImage,
    onClick: () -> Unit,
) {
    val aspectRatio = (widget.size.width / widget.size.height).coerceAtLeast(0.1f)
    DuaSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp).size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.widgets_dua_title, widget.displayNumber),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        widget.size.toWidgetCellSize().label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = widget.cropMode?.let { mode ->
                            stringResource(
                                if (mode == CropMode.FIT) {
                                    R.string.config_crop_fit
                                } else {
                                    R.string.config_crop_fill
                                },
                            )
                        } ?: stringResource(R.string.widgets_needs_image),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (widget.preview != null) {
                    Image(
                        bitmap = widget.preview.asImageBitmap(),
                        contentDescription = stringResource(
                            R.string.widgets_preview_description,
                            widget.displayNumber,
                        ),
                        modifier = if (aspectRatio >= 1f) {
                            Modifier.fillMaxWidth().aspectRatio(aspectRatio)
                        } else {
                            Modifier.fillMaxHeight().aspectRatio(aspectRatio)
                        }.clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.FillBounds,
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_image),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            stringResource(R.string.widgets_needs_image),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.widgets_edit),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PrayerWidgetSummaryCard(
    widget: ActiveWidgetSummary.PrayerTimes,
    onClick: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    DuaSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mosque),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp).size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.widgets_prayer_title, widget.displayNumber),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        widget.size.toWidgetCellSize().label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = widget.locationLabel ?: stringResource(R.string.prayer_no_location),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            ),
                        ),
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                val schedule = widget.schedule
                if (schedule != null) {
                    val now = System.currentTimeMillis()
                    val currentPrayer = schedule.currentPrayerAt(now)
                    val nextPrayer = schedule.nextPrayerAt(now)
                    val timeFormatter = SimpleDateFormat(
                        if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm",
                        androidx.compose.ui.platform.LocalConfiguration.current.locales[0],
                    ).apply {
                        timeZone = TimeZone.getTimeZone(schedule.timezoneId)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Top bar: Location & Hijri
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_mosque),
                                    contentDescription = null,
                                    tint = Gold300,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = widget.locationLabel ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White,
                                )
                            }
                            Text(
                                text = "${schedule.hijriDate.day} ${schedule.hijriDate.month} ${schedule.hijriDate.year} AH",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold300,
                            )
                        }

                        // Hero card: Next prayer & sunrise & LIVE badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(
                                            R.string.prayer_next_starts_in,
                                            nextPrayer.name.localizedName(context),
                                        ),
                                        color = androidx.compose.ui.graphics.Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.prayer_sunrise_value,
                                            timeFormatter.format(Date(schedule.sunriseEpochMillis)),
                                        ),
                                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
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
                                        color = androidx.compose.ui.graphics.Color(0xFF3E2723),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }

                        // 5 prayer slots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            schedule.prayers.forEach { prayer ->
                                val isCurrent = prayer.name == currentPrayer
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) Gold300 else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = prayer.name.localizedName(context),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = if (isCurrent) androidx.compose.ui.graphics.Color(0xFF3E2723) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        )
                                        Text(
                                            text = timeFormatter.format(Date(prayer.epochMillis)),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isCurrent) androidx.compose.ui.graphics.Color(0xFF3E2723) else androidx.compose.ui.graphics.Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mosque),
                            contentDescription = null,
                            tint = Gold300,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            stringResource(R.string.prayer_setup_title),
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.widgets_edit),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
