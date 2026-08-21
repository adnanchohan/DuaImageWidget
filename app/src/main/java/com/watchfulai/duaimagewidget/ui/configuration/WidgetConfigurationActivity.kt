package com.watchfulai.duaimagewidget.ui.configuration

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.watchfulai.duaimagewidget.MainActivity
import com.watchfulai.duaimagewidget.R
import com.watchfulai.duaimagewidget.data.AppSettings
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.data.CropMode
import com.watchfulai.duaimagewidget.data.CropTransform
import com.watchfulai.duaimagewidget.data.DEFAULT_WIDGET_BACKGROUND
import com.watchfulai.duaimagewidget.image.CropMath
import com.watchfulai.duaimagewidget.ui.components.DuaIconButton
import com.watchfulai.duaimagewidget.ui.components.DuaPrimaryButton
import com.watchfulai.duaimagewidget.ui.components.DuaSurfaceCard
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class WidgetConfigurationActivity : ComponentActivity() {
    private var isLeaving = false
    private val appSettingsRepository by lazy { AppSettingsRepository(applicationContext) }

    private val appWidgetId: Int by lazy {
        intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private val editorViewModel: WidgetConfigurationViewModel by lazy {
        ViewModelProvider(
            this,
            WidgetConfigurationViewModel.Factory(application, appWidgetId),
        )[WidgetConfigurationViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        onBackPressedDispatcher.addCallback(this) {
            leaveToHomeAfterAutoSave(RESULT_CANCELED)
        }
        enableEdgeToEdge()
        val widgetSize = currentWidgetSize(appWidgetId)

        setContent {
            val appSettings by appSettingsRepository.settings.collectAsState(initial = AppSettings())
            DuaImageWidgetTheme(appTheme = appSettings.theme) {
                val state by editorViewModel.uiState.collectAsState()
                WidgetConfigurationScreen(
                    state = state,
                    widgetSize = widgetSize,
                    onChooseImage = editorViewModel::importImage,
                    onCropModeChanged = editorViewModel::setCropMode,
                    onBackgroundColorChanged = editorViewModel::setBackgroundColor,
                    onCropTransformChanged = editorViewModel::setCropTransform,
                    onResetCrop = editorViewModel::resetCrop,
                    onAutoSaveCropChanged = editorViewModel::setAutoSaveCrop,
                    onDismissError = editorViewModel::dismissError,
                    onCancel = { leaveToHomeAfterAutoSave(RESULT_CANCELED) },
                    onOpenMainActivity = ::openMainActivityAfterAutoSave,
                    onSave = {
                        lifecycleScope.launch {
                            if (editorViewModel.save()) finishSuccessfully()
                        }
                    },
                )
            }
        }
    }

    private fun finishSuccessfully() {
        isLeaving = true
        finishToHome(RESULT_OK)
    }

    private fun leaveToHomeAfterAutoSave(resultCode: Int) {
        if (isLeaving) return
        isLeaving = true
        setWidgetResult(resultCode)
        openLauncher()
        lifecycleScope.launch {
            editorViewModel.flushAutoSave()
            finish()
        }
    }

    private fun finishToHome(resultCode: Int) {
        setWidgetResult(resultCode)
        openLauncher()
        finish()
    }

    private fun setWidgetResult(resultCode: Int) {
        setResult(
            resultCode,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
    }

    private fun openLauncher() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun openMainActivityAfterAutoSave() {
        if (isLeaving) return
        isLeaving = true
        openMainActivity()
        lifecycleScope.launch {
            editorViewModel.flushAutoSave()
            finish()
        }
    }

    private fun openMainActivity() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
        )
    }

    private fun currentWidgetSize(id: Int): WidgetSizeDp {
        val options = AppWidgetManager.getInstance(this).getAppWidgetOptions(id)
        return resolveWidgetSize(
            exactSize = intent.widgetSizeOrNull(),
            minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
            minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
            maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
            maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
            isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
        )
    }
}

@Composable
private fun WidgetConfigurationScreen(
    state: WidgetEditorUiState,
    widgetSize: WidgetSizeDp,
    onChooseImage: (android.net.Uri) -> Unit,
    onCropModeChanged: (CropMode) -> Unit,
    onBackgroundColorChanged: (Int) -> Unit,
    onCropTransformChanged: (CropTransform) -> Unit,
    onResetCrop: () -> Unit,
    onAutoSaveCropChanged: (Boolean) -> Unit,
    onDismissError: () -> Unit,
    onCancel: () -> Unit,
    onOpenMainActivity: () -> Unit,
    onSave: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onChooseImage) },
    )
    val chooseImage = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val screenScrollState = rememberScrollState()
    var cropGestureActive by remember { mutableStateOf(false) }
    val currentCellSize = remember(widgetSize) { widgetSize.toWidgetCellSize() }
    val availableCellSizes = remember(currentCellSize) {
        availableWidgetCellSizes(currentCellSize)
    }
    var selectedCellSize by remember(currentCellSize) { mutableStateOf(currentCellSize) }
    val previewWidgetSize = remember(widgetSize, currentCellSize, selectedCellSize) {
        if (selectedCellSize == currentCellSize) {
            widgetSize
        } else {
            selectedCellSize.toPreviewSizeDp()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DuaPrimaryButton(
                        text = when {
                            state.isSaving -> "Saving…"
                            state.autoSaveCrop && state.hasPersistedConfiguration -> "Done"
                            else -> "Save widget"
                        },
                        onClick = onSave,
                        enabled = state.bitmap != null && !state.isSaving && !state.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                        leading = if (state.isSaving) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        } else {
                            null
                        },
                    )
                    OutlinedButton(
                        onClick = onOpenMainActivity,
                        enabled = !state.isSaving && !state.isImporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Open app · add another widget")
                    }
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(screenScrollState, enabled = !cropGestureActive)
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DuaIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = "Cancel and return home",
                    onClick = onCancel,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Frame your dua", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (selectedCellSize == currentCellSize) {
                            "Previewing the exact current home-screen size."
                        } else {
                            "Previewing the ${selectedCellSize.label} target size."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Live preview", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (selectedCellSize == currentCellSize) {
                            "Current home size"
                        } else {
                            "Selected ${selectedCellSize.label}"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                WidgetSizeStrip(
                    sizes = availableCellSizes,
                    current = currentCellSize,
                    selected = selectedCellSize,
                    onSelected = { selectedCellSize = it },
                )
            }

            if (selectedCellSize != currentCellSize) {
                ResizeInstruction(
                    target = selectedCellSize,
                    onReturnHome = onCancel,
                    canReturnHome = state.hasPersistedConfiguration,
                )
            }

            when {
                state.isLoading -> LoadingPanel("Loading your widget…")
                state.isImporting -> LoadingPanel("Preparing your image…")
                state.bitmap == null -> EmptyImagePanel(onChoose = chooseImage)
                else -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CropPreview(
                                bitmap = state.bitmap,
                                widgetSize = previewWidgetSize,
                                cropMode = state.cropMode,
                                transform = state.cropTransform,
                                backgroundColor = state.backgroundColor,
                                onTransformChanged = onCropTransformChanged,
                                onGestureActiveChanged = { cropGestureActive = it },
                            )
                            if (state.cropMode == CropMode.FILL) {
                                Text(
                                    "Drag to position · pinch to zoom",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }

                    DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_crop),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text("Image display", style = MaterialTheme.typography.titleMedium)
                            }
                            CropModeSelector(
                                selected = state.cropMode,
                                onSelected = onCropModeChanged,
                            )
                            Text(
                                if (state.cropMode == CropMode.FIT) {
                                    "Fit protects every line and uses your chosen background around the image."
                                } else {
                                    "Fill uses the whole frame. Keep important text inside the rounded preview."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (state.cropMode == CropMode.FIT) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Fit background",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        FIT_BACKGROUND_COLORS.forEach { color ->
                                            FitBackgroundSwatch(
                                                color = color,
                                                selected = state.backgroundColor == color,
                                                onClick = { onBackgroundColorChanged(color) },
                                            )
                                        }
                                    }
                                    Text(
                                        "Choose the color visible around images in Fit mode.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_image),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-save edits", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (state.hasPersistedConfiguration) {
                                        "Apply image, crop, zoom and position changes automatically."
                                    } else {
                                        "Auto-save starts after this widget is saved once."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(
                                checked = state.autoSaveCrop,
                                onCheckedChange = onAutoSaveCropChanged,
                                enabled = !state.isSaving && !state.isImporting,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = chooseImage,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Change image")
                        }
                        OutlinedButton(
                            onClick = onResetCrop,
                            enabled = state.cropMode == CropMode.FILL,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Reset crop")
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Dismiss",
                            modifier = Modifier.clickable(onClick = onDismissError),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetSizeStrip(
    sizes: List<WidgetCellSize>,
    current: WidgetCellSize,
    selected: WidgetCellSize,
    onSelected: (WidgetCellSize) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sizes.forEach { size ->
            val isSelected = size == selected
            val isCurrent = size == current
            Surface(
                modifier = Modifier.clickable { onSelected(size) },
                shape = CircleShape,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected || isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Text(
                    text = if (isCurrent) "${size.label} · current" else size.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ResizeInstruction(
    target: WidgetCellSize,
    onReturnHome: () -> Unit,
    canReturnHome: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "Previewing ${target.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (canReturnHome) {
                        "Return home and long-press the widget to resize to ${target.label}."
                    } else {
                        "Save this widget first. Then return home and long-press it to resize to ${target.label}."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (canReturnHome) {
                Text(
                    text = "Home",
                    modifier = Modifier.clickable(onClick = onReturnHome),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LoadingPanel(label: String) {
    DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(14.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyImagePanel(onChoose: () -> Unit) {
    DuaSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(28.dp),
                )
            }
            Text(
                "Choose your dua image",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                "Select any small dua image. It is copied to private storage so your widget remains reliable.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            DuaPrimaryButton(
                text = "Choose image",
                onClick = onChoose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CropModeSelector(
    selected: CropMode,
    onSelected: (CropMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CropModeOption(
            label = "Fit",
            selected = selected == CropMode.FIT,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(CropMode.FIT) },
        )
        CropModeOption(
            label = "Fill & crop",
            selected = selected == CropMode.FILL,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(CropMode.FILL) },
        )
    }
}

@Composable
private fun FitBackgroundSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ringColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
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
                        if (color == DARK_FIT_BACKGROUND) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun CropModeOption(
    label: String,
    selected: Boolean,
    modifier: Modifier,
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
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CropPreview(
    bitmap: Bitmap,
    widgetSize: WidgetSizeDp,
    cropMode: CropMode,
    transform: CropTransform,
    backgroundColor: Int,
    onTransformChanged: (CropTransform) -> Unit,
    onGestureActiveChanged: (Boolean) -> Unit,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val latestTransform by rememberUpdatedState(transform)
    val latestOnTransformChanged by rememberUpdatedState(onTransformChanged)
    val latestOnGestureActiveChanged by rememberUpdatedState(onGestureActiveChanged)
    val frameShape = RoundedCornerShape(18.dp)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val previewWidth = widgetSize.width.dp
        val previewHeight = widgetSize.height.dp
        val centeringSpace = ((maxWidth - previewWidth) / 2).coerceAtLeast(0.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = centeringSpace),
        ) {
            Canvas(
                modifier = Modifier
                    .width(previewWidth)
                    .height(previewHeight)
                    .clip(frameShape)
                    .background(Color(backgroundColor))
                    .border(2.dp, MaterialTheme.colorScheme.primary, frameShape)
                    .pointerInput(bitmap, cropMode) {
                        if (cropMode == CropMode.FILL) {
                            awaitEachGesture {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                latestOnGestureActiveChanged(true)
                                var gestureTransform = latestTransform
                                try {
                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val pan = event.calculatePan()
                                        val zoom = event.calculateZoom()
                                        if (pan.x != 0f || pan.y != 0f || zoom != 1f) {
                                            event.changes.forEach { it.consume() }
                                            gestureTransform = CropMath.transformed(
                                                current = gestureTransform,
                                                panX = pan.x,
                                                panY = pan.y,
                                                zoomChange = zoom,
                                                sourceWidth = bitmap.width,
                                                sourceHeight = bitmap.height,
                                                targetWidth = size.width,
                                                targetHeight = size.height,
                                            )
                                            latestOnTransformChanged(gestureTransform)
                                        }
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    latestOnGestureActiveChanged(false)
                                }
                            }
                        }
                    },
            ) {
                val geometry = CropMath.geometry(
                    sourceWidth = bitmap.width,
                    sourceHeight = bitmap.height,
                    targetWidth = size.width.roundToInt(),
                    targetHeight = size.height.roundToInt(),
                    mode = cropMode,
                    transform = transform,
                )
                drawImage(
                    image = image,
                    srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                    srcSize = androidx.compose.ui.unit.IntSize(bitmap.width, bitmap.height),
                    dstOffset = androidx.compose.ui.unit.IntOffset(
                        geometry.left.roundToInt(),
                        geometry.top.roundToInt(),
                    ),
                    dstSize = androidx.compose.ui.unit.IntSize(
                        geometry.width.roundToInt(),
                        geometry.height.roundToInt(),
                    ),
                    filterQuality = FilterQuality.High,
                )
            }
        }
    }
}

private const val DARK_FIT_BACKGROUND = 0xFF17201D.toInt()
private val FIT_BACKGROUND_COLORS = listOf(
    DEFAULT_WIDGET_BACKGROUND,
    0xFFFFFFFF.toInt(),
    0xFFE4F3EE.toInt(),
    DARK_FIT_BACKGROUND,
)
