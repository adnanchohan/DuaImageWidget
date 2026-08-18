package com.watchfulai.duaimagewidget.ui.configuration

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.watchfulai.duaimagewidget.data.CropMode
import com.watchfulai.duaimagewidget.data.CropTransform
import com.watchfulai.duaimagewidget.data.DEFAULT_WIDGET_BACKGROUND
import com.watchfulai.duaimagewidget.data.WidgetConfig
import com.watchfulai.duaimagewidget.data.WidgetConfigRepository
import com.watchfulai.duaimagewidget.image.ImageStorage
import com.watchfulai.duaimagewidget.widget.DuaImageWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WidgetEditorUiState(
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val isSaving: Boolean = false,
    val imageFileName: String? = null,
    val bitmap: Bitmap? = null,
    val cropMode: CropMode = CropMode.FIT,
    val cropTransform: CropTransform = CropTransform(),
    val backgroundColor: Int = DEFAULT_WIDGET_BACKGROUND,
    val errorMessage: String? = null,
)

class WidgetConfigurationViewModel(
    application: Application,
    private val appWidgetId: Int,
) : AndroidViewModel(application) {
    private val repository = WidgetConfigRepository(application)
    private val _uiState = MutableStateFlow(WidgetEditorUiState())
    val uiState: StateFlow<WidgetEditorUiState> = _uiState.asStateFlow()

    private var originalImageFileName: String? = null
    private var pendingImageFileName: String? = null

    init {
        viewModelScope.launch {
            val existing = repository.get(appWidgetId)
            originalImageFileName = existing?.imageFileName
            val bitmap = existing?.let { ImageStorage.load(application, it.imageFileName) }
            _uiState.value = WidgetEditorUiState(
                isLoading = false,
                imageFileName = existing?.imageFileName,
                bitmap = bitmap,
                cropMode = existing?.cropMode ?: CropMode.FIT,
                cropTransform = existing?.cropTransform ?: CropTransform(),
                backgroundColor = existing?.backgroundColor ?: DEFAULT_WIDGET_BACKGROUND,
                errorMessage = if (existing != null && bitmap == null) {
                    "The previous image is no longer available. Please choose it again."
                } else {
                    null
                },
            )
        }
    }

    fun importImage(uri: Uri) {
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }
            runCatching { ImageStorage.import(getApplication(), uri) }
                .onSuccess { stored ->
                    pendingImageFileName?.let { ImageStorage.delete(getApplication(), it) }
                    pendingImageFileName = stored.fileName
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            imageFileName = stored.fileName,
                            bitmap = stored.bitmap,
                            cropMode = CropMode.FIT,
                            cropTransform = CropTransform(),
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = throwable.message ?: "The image could not be imported.",
                        )
                    }
                }
        }
    }

    fun setCropMode(mode: CropMode) {
        _uiState.update { it.copy(cropMode = mode) }
    }

    fun setCropTransform(transform: CropTransform) {
        _uiState.update { it.copy(cropTransform = transform) }
    }

    fun resetCrop() {
        _uiState.update { it.copy(cropTransform = CropTransform()) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    suspend fun save(): Boolean {
        val state = _uiState.value
        val imageFileName = state.imageFileName ?: return false
        if (state.isSaving) return false

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        return runCatching {
            repository.save(
                WidgetConfig(
                    appWidgetId = appWidgetId,
                    imageFileName = imageFileName,
                    cropMode = state.cropMode,
                    cropTransform = state.cropTransform,
                    backgroundColor = state.backgroundColor,
                ),
            )
            if (originalImageFileName != null && originalImageFileName != imageFileName) {
                ImageStorage.delete(getApplication(), originalImageFileName!!)
            }
            originalImageFileName = imageFileName
            pendingImageFileName = null
            DuaImageWidget().update(getApplication(), appWidgetId)
        }.fold(
            onSuccess = {
                _uiState.update { it.copy(isSaving = false) }
                true
            },
            onFailure = { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "The widget could not be saved.",
                    )
                }
                false
            },
        )
    }

    override fun onCleared() {
        pendingImageFileName?.let { ImageStorage.delete(getApplication(), it) }
    }

    class Factory(
        private val application: Application,
        private val appWidgetId: Int,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WidgetConfigurationViewModel(application, appWidgetId) as T
        }
    }
}
