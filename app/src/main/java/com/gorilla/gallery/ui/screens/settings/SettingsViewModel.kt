package com.gorilla.gallery.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.gallery.AppContainer
import com.gorilla.gallery.data.settings.AccentChoice
import com.gorilla.gallery.data.settings.DateGranularity
import com.gorilla.gallery.ui.theme.ThemeMode
import com.gorilla.gallery.ui.viewModelFactory
import kotlinx.coroutines.launch

/** Thin write-through wrapper around SettingsRepository — every control writes immediately. */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.settingsRepository

    fun setThemeMode(v: ThemeMode) = viewModelScope.launch { repo.setThemeMode(v) }
    fun setAccent(v: AccentChoice) = viewModelScope.launch { repo.setAccent(v) }
    fun setBlurIntensity(v: com.gorilla.gallery.ui.theme.BlurIntensity) = viewModelScope.launch { repo.setBlurIntensity(v) }
    fun setLiquidGlassIntensity(v: com.gorilla.gallery.ui.theme.BlurIntensity) = viewModelScope.launch { repo.setLiquidGlassIntensity(v) }
    fun setSurfaceOpacity(v: Float) = viewModelScope.launch { repo.setSurfaceOpacity(v) }
    fun setGridColumns(v: Int) = viewModelScope.launch { repo.setGridColumns(v) }
    fun setDateGranularity(v: DateGranularity) = viewModelScope.launch { repo.setDateGranularity(v) }
    fun setHighQualityThumbnails(v: Boolean) = viewModelScope.launch { repo.setHighQualityThumbnails(v) }

    fun setSecureEnabled(v: Boolean) = viewModelScope.launch { repo.setSecureFolderEnabled(v) }
    fun setBiometricUnlock(v: Boolean) = viewModelScope.launch { repo.setBiometricUnlock(v) }
    fun setShowSecureInAlbums(v: Boolean) = viewModelScope.launch { repo.setShowSecureInAlbums(v) }
    fun setPin(pin: String) = viewModelScope.launch { repo.setPin(pin) }
    fun clearPin() = viewModelScope.launch { repo.clearPin() }

    companion object {
        val Factory = viewModelFactory { container -> SettingsViewModel(container) }
    }
}
