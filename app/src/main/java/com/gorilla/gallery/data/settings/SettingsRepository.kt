package com.gorilla.gallery.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gorilla.gallery.ui.theme.BlurIntensity
import com.gorilla.gallery.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gorilla_gallery_settings")

/**
 * Single source of truth for all user settings, persisted with DataStore. Every
 * Settings control reads from [settings] and writes through one update method;
 * changes propagate immediately to the UI via the theme composition locals.
 *
 * The Secure Folder PIN is stored only as a salted SHA-256 hash — never plaintext.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val blur = stringPreferencesKey("blur_intensity")
        val liquidGlassIntensity = stringPreferencesKey("liquid_glass_intensity")
        val surfaceOpacity = androidx.datastore.preferences.core.floatPreferencesKey("surface_opacity")
        val gridColumns = intPreferencesKey("grid_columns")
        val dateGranularity = stringPreferencesKey("date_granularity")
        val highQualityThumbnails = booleanPreferencesKey("high_quality_thumbnails")
        val secureEnabled = booleanPreferencesKey("secure_enabled")
        val biometricUnlock = booleanPreferencesKey("biometric_unlock")
        val showSecureInAlbums = booleanPreferencesKey("show_secure_in_albums")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.AUTO,
            accent = p[Keys.accent]?.let { runCatching { AccentChoice.valueOf(it) }.getOrNull() } ?: AccentChoice.ADAPTIVE,
            blurIntensity = p[Keys.blur]?.let { runCatching { BlurIntensity.valueOf(it) }.getOrNull() } ?: BlurIntensity.MEDIUM,
            liquidGlassIntensity = p[Keys.liquidGlassIntensity]?.let { runCatching { BlurIntensity.valueOf(it) }.getOrNull() } ?: BlurIntensity.MEDIUM,
            surfaceOpacity = p[Keys.surfaceOpacity] ?: 0.4f,
            gridColumns = (p[Keys.gridColumns] ?: 3).coerceIn(2, 6),
            dateGranularity = p[Keys.dateGranularity]?.let { runCatching { DateGranularity.valueOf(it) }.getOrNull() } ?: DateGranularity.ALL,
            highQualityThumbnails = p[Keys.highQualityThumbnails] ?: false,
            secureFolderEnabled = p[Keys.secureEnabled] ?: false,
            biometricUnlock = p[Keys.biometricUnlock] ?: true,
            showSecureInAlbums = p[Keys.showSecureInAlbums] ?: true,
            pinHash = p[Keys.pinHash],
            pinSalt = p[Keys.pinSalt],
        )
    }



    suspend fun setThemeMode(v: ThemeMode) = edit { it[Keys.themeMode] = v.name }
    suspend fun setAccent(v: AccentChoice) = edit { it[Keys.accent] = v.name }
    suspend fun setBlurIntensity(v: BlurIntensity) = edit { it[Keys.blur] = v.name }
    suspend fun setLiquidGlassIntensity(v: BlurIntensity) = edit { it[Keys.liquidGlassIntensity] = v.name }
    suspend fun setSurfaceOpacity(v: Float) = edit { it[Keys.surfaceOpacity] = v }
    suspend fun setGridColumns(v: Int) = edit { it[Keys.gridColumns] = v.coerceIn(2, 6) }
    suspend fun setDateGranularity(v: DateGranularity) = edit { it[Keys.dateGranularity] = v.name }
    suspend fun setHighQualityThumbnails(v: Boolean) = edit { it[Keys.highQualityThumbnails] = v }

    suspend fun setSecureFolderEnabled(v: Boolean) = edit { it[Keys.secureEnabled] = v }
    suspend fun setBiometricUnlock(v: Boolean) = edit { it[Keys.biometricUnlock] = v }
    suspend fun setShowSecureInAlbums(v: Boolean) = edit { it[Keys.showSecureInAlbums] = v }

    /** Generate a fresh salt and store salted SHA-256(pin). */
    suspend fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        edit {
            it[Keys.pinSalt] = base64(salt)
            it[Keys.pinHash] = hash
        }
    }

    suspend fun clearPin() = edit {
        it.remove(Keys.pinHash)
        it.remove(Keys.pinSalt)
    }

    /** Constant-time verify against the stored salted hash. */
    fun verifyPin(pin: String, settings: AppSettings): Boolean {
        val saltB64 = settings.pinSalt ?: return false
        val stored = settings.pinHash ?: return false
        val computed = hash(pin, unbase64(saltB64))
        return MessageDigest.isEqual(stored.toByteArray(), computed.toByteArray())
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return base64(md.digest(pin.toByteArray(Charsets.UTF_8)))
    }

    private fun base64(b: ByteArray) = android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP)
    private fun unbase64(s: String) = android.util.Base64.decode(s, android.util.Base64.NO_WRAP)

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
