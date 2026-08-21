package com.watchfulai.duaimagewidget.ui.widgets

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
import androidx.lifecycle.lifecycleScope
import com.watchfulai.duaimagewidget.R
import com.watchfulai.duaimagewidget.data.AppSettings
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.data.CropMode
import com.watchfulai.duaimagewidget.data.WidgetConfigRepository
import com.watchfulai.duaimagewidget.image.ImageStorage
import com.watchfulai.duaimagewidget.image.WidgetBitmapRenderer
import com.watchfulai.duaimagewidget.ui.LocaleAwareActivity
import com.watchfulai.duaimagewidget.ui.components.BrandMark
import com.watchfulai.duaimagewidget.ui.components.DuaIconButton
import com.watchfulai.duaimagewidget.ui.components.DuaPrimaryButton
import com.watchfulai.duaimagewidget.ui.components.DuaSurfaceCard
import com.watchfulai.duaimagewidget.ui.configuration.EXTRA_EDIT_FROM_WIDGET_LIST
import com.watchfulai.duaimagewidget.ui.configuration.WidgetConfigurationActivity
import com.watchfulai.duaimagewidget.ui.configuration.WidgetSizeDp
import com.watchfulai.duaimagewidget.ui.configuration.resolveWidgetSize
import com.watchfulai.duaimagewidget.ui.configuration.toWidgetCellSize
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import com.watchfulai.duaimagewidget.widget.DuaImageWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class YourWidgetsActivity : LocaleAwareActivity() {
    private val settingsRepository by lazy { AppSettingsRepository(applicationContext) }
    private val widgetRepository by lazy { WidgetConfigRepository(applicationContext) }
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

    private suspend fun loadActiveWidgets(): List<WidgetSummary> {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val provider = ComponentName(applicationContext, DuaImageWidgetReceiver::class.java)
        val isLandscape = resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE

        return manager.getAppWidgetIds(provider)
            .sorted()
            .mapIndexed { index, appWidgetId ->
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
                WidgetSummary(
                    appWidgetId = appWidgetId,
                    displayNumber = index + 1,
                    size = size,
                    cropMode = config?.cropMode,
                    preview = config?.let { savedConfig ->
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
                    },
                )
            }
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

    private fun editWidget(appWidgetId: Int) {
        startActivity(
            Intent(this, WidgetConfigurationActivity::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_EDIT_FROM_WIDGET_LIST, true)
            },
        )
    }

    private companion object {
        const val PREVIEW_LONG_EDGE_PX = 720
        const val FALLBACK_CORNER_RADIUS_DP = 20f
    }
}

private data class YourWidgetsState(
    val isLoading: Boolean = true,
    val items: List<WidgetSummary> = emptyList(),
)

private data class WidgetSummary(
    val appWidgetId: Int,
    val displayNumber: Int,
    val size: WidgetSizeDp,
    val cropMode: CropMode?,
    val preview: Bitmap?,
)

@Composable
private fun YourWidgetsScreen(
    state: YourWidgetsState,
    onBack: () -> Unit,
    onEditWidget: (Int) -> Unit,
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
                        WidgetSummaryCard(
                            widget = widget,
                            onClick = { onEditWidget(widget.appWidgetId) },
                        )
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
private fun WidgetSummaryCard(
    widget: WidgetSummary,
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
                        painter = painterResource(R.drawable.ic_widgets),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp).size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.widgets_item_title, widget.displayNumber),
                        style = MaterialTheme.typography.titleMedium,
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
