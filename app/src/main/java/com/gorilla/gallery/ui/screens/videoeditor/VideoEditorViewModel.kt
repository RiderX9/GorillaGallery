package com.gorilla.gallery.ui.screens.videoeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.geometry.Rect
import com.gorilla.gallery.AppContainer
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoEditorViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.videoEditorRepository

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _appliedCropRect = MutableStateFlow<Rect?>(null)
    val appliedCropRect: StateFlow<Rect?> = _appliedCropRect.asStateFlow()

    fun applyCrop(rect: Rect?) {
        _appliedCropRect.value = rect
    }

    fun saveCopy(
        app: AppViewModel,
        item: MediaItem,
        startMs: Long,
        endMs: Long,
        muted: Boolean,
        replacementAudioUri: android.net.Uri?,
        audioStartMs: Long,
        cropRect: android.graphics.RectF?,
        filterArgb: Int?,
        rotationDegrees: Int = 0,
        onDone: () -> Unit,
    ) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            runCatching {
                repo.saveEditedCopy(
                    item = item,
                    startMs = startMs,
                    endMs = endMs,
                    removeAudio = muted || replacementAudioUri != null,
                    replacementAudioUri = replacementAudioUri,
                    audioStartMs = audioStartMs,
                    cropRect = cropRect,
                    filterArgb = filterArgb,
                    rotationDegrees = rotationDegrees,
                )
            }.onSuccess {
                app.showSnackbar.tryEmit("Video saved")
                container.mediaRepository.scan()
                onDone()
            }.onFailure {
                app.showSnackbar.tryEmit("Video export failed")
            }
            _saving.value = false
        }
    }

    fun overwrite(
        app: AppViewModel,
        item: MediaItem,
        startMs: Long,
        endMs: Long,
        muted: Boolean,
        replacementAudioUri: android.net.Uri?,
        audioStartMs: Long,
        cropRect: android.graphics.RectF?,
        filterArgb: Int?,
        rotationDegrees: Int = 0,
        onDone: () -> Unit,
    ) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            runCatching {
                repo.saveEditedCopy(
                    item = item,
                    startMs = startMs,
                    endMs = endMs,
                    removeAudio = muted || replacementAudioUri != null,
                    replacementAudioUri = replacementAudioUri,
                    audioStartMs = audioStartMs,
                    cropRect = cropRect,
                    filterArgb = filterArgb,
                    rotationDegrees = rotationDegrees,
                    overwrite = true,
                )
            }.onSuccess {
                app.showSnackbar.tryEmit("Video overwritten")
                container.mediaRepository.scan()
                onDone()
            }.onFailure {
                app.showSnackbar.tryEmit("Video export failed")
            }
            _saving.value = false
        }
    }

    companion object {
        val Factory = viewModelFactory { container -> VideoEditorViewModel(container) }
    }
}
