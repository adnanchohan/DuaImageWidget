package com.watchfulai.duaimagewidget.ui.configuration

import android.appwidget.AppWidgetManager
import android.content.res.Configuration
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.watchfulai.duaimagewidget.data.CropMode
import com.watchfulai.duaimagewidget.data.CropTransform
import com.watchfulai.duaimagewidget.image.CropMath
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class WidgetConfigurationActivity : ComponentActivity() {
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
        enableEdgeToEdge()
        val widgetSize = currentWidgetSize(appWidgetId)

        setContent {
            DuaImageWidgetTheme(dynamicColor = false) {
                val state by editorViewModel.uiState.collectAsState()
                WidgetConfigurationScreen(
                    state = state,
                    widgetSize = widgetSize,
                    onChooseImage = editorViewModel::importImage,
                    onCropModeChanged = editorViewModel::setCropMode,
                    onCropTransformChanged = editorViewModel::setCropTransform,
                    onResetCrop = editorViewModel::resetCrop,
                    onDismissError = editorViewModel::dismissError,
                    onCancel = ::finish,
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
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
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
    onCropTransformChanged: (CropTransform) -> Unit,
    onResetCrop: () -> Unit,
    onDismissError: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onChooseImage) },
    )

    Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Set your dua image", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Preview and position it for this widget.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onCancel, enabled = !state.isSaving) {
                    Text("Cancel")
                }
            }

            when {
                state.isLoading -> LoadingPanel("Loading widget…")
                state.isImporting -> LoadingPanel("Preparing image…")
                state.bitmap == null -> EmptyImagePanel(
                    onChoose = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                else -> {
                    CropPreview(
                        bitmap = state.bitmap,
                        widgetSize = widgetSize,
                        cropMode = state.cropMode,
                        transform = state.cropTransform,
                        backgroundColor = state.backgroundColor,
                        onTransformChanged = onCropTransformChanged,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Image display", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ChoiceButton(
                                label = "Fit",
                                selected = state.cropMode == CropMode.FIT,
                                modifier = Modifier.weight(1f),
                                onClick = { onCropModeChanged(CropMode.FIT) },
                            )
                            ChoiceButton(
                                label = "Fill & crop",
                                selected = state.cropMode == CropMode.FILL,
                                modifier = Modifier.weight(1f),
                                onClick = { onCropModeChanged(CropMode.FILL) },
                            )
                        }
                        Text(
                            if (state.cropMode == CropMode.FIT) {
                                "Fit keeps every line visible. Empty space uses a soft background."
                            } else {
                                "Drag to reposition and pinch to zoom. Keep all important text inside the frame."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                picker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Change image")
                        }
                        OutlinedButton(
                            onClick = onResetCrop,
                            enabled = state.cropMode == CropMode.FILL,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Reset crop")
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = onDismissError) { Text("Dismiss") }
                }
            }

            Button(
                onClick = onSave,
                enabled = state.bitmap != null && !state.isSaving && !state.isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(10.dp))
                }
                Text(if (state.isSaving) "Saving…" else "Save widget")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LoadingPanel(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(label)
        }
    }
}

@Composable
private fun EmptyImagePanel(onChoose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Choose a dua image to begin.", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your image is copied into private app storage so the widget keeps working.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onChoose) { Text("Choose image") }
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
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
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
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
                    .pointerInput(bitmap, cropMode, transform) {
                        if (cropMode == CropMode.FILL) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                onTransformChanged(
                                    CropMath.transformed(
                                        current = transform,
                                        panX = pan.x,
                                        panY = pan.y,
                                        zoomChange = zoom,
                                        sourceWidth = bitmap.width,
                                        sourceHeight = bitmap.height,
                                        targetWidth = size.width,
                                        targetHeight = size.height,
                                    ),
                                )
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
