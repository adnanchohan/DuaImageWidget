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
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.data.WidgetConfig
import com.watchfulai.duaimagewidget.data.WidgetConfigRepository
import com.watchfulai.duaimagewidget.image.ImageStorage
import com.watchfulai.duaimagewidget.widget.DuaImageWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class WidgetEditorUiState(
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val isSaving: Boolean = false,
    val imageFileName: String? = null,
    val bitmap: Bitmap? = null,
    val cropMode: CropMode = CropMode.FIT,
    val cropTransform: CropTransform = CropTransform(),
    val backgroundColor: Int = DEFAULT_WIDGET_BACKGROUND,
    val autoSaveCrop: Boolean = false,
    val hasPersistedConfiguration: Boolean = false,
    val errorMessage: String? = null,
)

class WidgetConfigurationViewModel(
    application: Application,
    private val appWidgetId: Int,
) : AndroidViewModel(application) {
    private val repository = WidgetConfigRepository(application)
    private val appSettingsRepository = AppSettingsRepository(application)
    private val _uiState = MutableStateFlow(WidgetEditorUiState())
    val uiState: StateFlow<WidgetEditorUiState> = _uiState.asStateFlow()

    private var originalImageFileName: String? = null
    private var pendingImageFileName: String? = null
    private var autoSaveJob: Job? = null
    private var autoSaveRevision = 0L
    private var persistedAutoSaveRevision = 0L
    private val persistenceMutex = Mutex()

    init {
        viewModelScope.launch {
            val existing = repository.get(appWidgetId)
            val defaults = appSettingsRepository.settings.first()
            originalImageFileName = existing?.imageFileName
            val bitmap = existing?.let { ImageStorage.load(application, it.imageFileName) }
            _uiState.value = WidgetEditorUiState(
                isLoading = false,
                imageFileName = existing?.imageFileName,
                bitmap = bitmap,
                cropMode = existing?.cropMode ?: defaults.defaultCropMode,
                cropTransform = existing?.cropTransform ?: CropTransform(),
                backgroundColor = existing?.backgroundColor ?: defaults.defaultWidgetBackground,
                autoSaveCrop = existing?.autoSaveCrop ?: defaults.autoSaveCropByDefault,
                hasPersistedConfiguration = existing != null,
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
                            cropTransform = CropTransform(),
                        )
                    }
                    scheduleAutoSave()
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
        scheduleAutoSave()
    }

    fun setBackgroundColor(color: Int) {
        if (_uiState.value.backgroundColor == color) return
        _uiState.update { it.copy(backgroundColor = color) }
        scheduleAutoSave()
    }

    fun setCropTransform(transform: CropTransform) {
        _uiState.update { it.copy(cropTransform = transform) }
        scheduleAutoSave()
    }

    fun resetCrop() {
        _uiState.update { it.copy(cropTransform = CropTransform()) }
        scheduleAutoSave()
    }

    fun setAutoSaveCrop(enabled: Boolean) {
        if (_uiState.value.autoSaveCrop == enabled) return
        _uiState.update { it.copy(autoSaveCrop = enabled, errorMessage = null) }
        if (_uiState.value.hasPersistedConfiguration) {
            scheduleAutoSave(immediate = true, requireAutoSaveEnabled = false)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    suspend fun save(): Boolean {
        autoSaveJob?.cancel()
        autoSaveJob?.join()
        val state = _uiState.value
        if (state.imageFileName == null) return false
        if (state.isSaving) return false

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        return try {
            persist(state)
            persistedAutoSaveRevision = autoSaveRevision
            _uiState.update {
                it.copy(isSaving = false, hasPersistedConfiguration = true)
            }
            true
        } catch (cancellation: CancellationException) {
            _uiState.update { it.copy(isSaving = false) }
            throw cancellation
        } catch (throwable: Throwable) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessage = throwable.message ?: "The widget could not be saved.",
                )
            }
            false
        }
    }

    private fun scheduleAutoSave(
        immediate: Boolean = false,
        requireAutoSaveEnabled: Boolean = true,
    ) {
        val current = _uiState.value
        if (!current.hasPersistedConfiguration || current.imageFileName == null) return
        if (requireAutoSaveEnabled && !current.autoSaveCrop) return

        val revision = ++autoSaveRevision
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            if (!immediate) delay(AUTO_SAVE_DEBOUNCE_MILLIS)

            val snapshot = _uiState.value
            if (!snapshot.hasPersistedConfiguration || snapshot.imageFileName == null) return@launch
            if (requireAutoSaveEnabled && !snapshot.autoSaveCrop) return@launch

            try {
                persist(snapshot)
                persistedAutoSaveRevision = maxOf(persistedAutoSaveRevision, revision)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        errorMessage = throwable.message ?: "Auto-save could not update the widget.",
                    )
                }
            }
        }
    }

    suspend fun flushAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob?.join()
        if (persistedAutoSaveRevision >= autoSaveRevision) return

        val snapshot = _uiState.value
        if (!snapshot.hasPersistedConfiguration || snapshot.imageFileName == null) return

        try {
            persist(snapshot)
            persistedAutoSaveRevision = autoSaveRevision
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            _uiState.update {
                it.copy(
                    errorMessage = throwable.message ?: "Auto-save could not update the widget.",
                )
            }
        }
    }

    private suspend fun persist(state: WidgetEditorUiState) {
        val imageFileName = checkNotNull(state.imageFileName)
        persistenceMutex.withLock {
            withContext(NonCancellable) {
                repository.save(
                    WidgetConfig(
                        appWidgetId = appWidgetId,
                        imageFileName = imageFileName,
                        cropMode = state.cropMode,
                        cropTransform = state.cropTransform,
                        backgroundColor = state.backgroundColor,
                        autoSaveCrop = state.autoSaveCrop,
                    ),
                )
                if (originalImageFileName != null && originalImageFileName != imageFileName) {
                    ImageStorage.delete(getApplication(), originalImageFileName!!)
                }
                originalImageFileName = imageFileName
                pendingImageFileName = null
                DuaImageWidget().update(getApplication(), appWidgetId)
            }
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
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

    private companion object {
        const val AUTO_SAVE_DEBOUNCE_MILLIS = 350L
    }
}
