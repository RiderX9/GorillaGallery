package com.gorilla.gallery

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import com.gorilla.gallery.BuildConfig
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.BiometricGateway
import com.gorilla.gallery.ui.ConsentRequest
import com.gorilla.gallery.ui.GalleryRoot
import com.gorilla.gallery.ui.LocalBiometricGateway
import com.gorilla.gallery.ui.theme.GorillaTheme
import com.gorilla.gallery.ui.theme.ThemeMode

class MainActivity : FragmentActivity(), BiometricGateway {

    private var backgroundedAt: Long = 0L
    private var activeAppViewModel: AppViewModel? = null

    override fun onStop() {
        super.onStop()
        backgroundedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAt > 0L) {
            val gap = System.currentTimeMillis() - backgroundedAt
            activeAppViewModel?.onAppResumed(gap)
            backgroundedAt = 0L
        }
    }

    // ---- BiometricGateway -----------------------------------------------------

    private val allowed = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    override fun canAuthenticate(): Boolean =
        BiometricManager.from(this).canAuthenticate(allowed) == BiometricManager.BIOMETRIC_SUCCESS

    override fun authenticate(onResult: (Boolean) -> Unit) {
        if (!canAuthenticate()) { onResult(false); return }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
            // onAuthenticationFailed = single mismatch; keep the prompt up, no callback.
        })
        // NOTE: with DEVICE_CREDENTIAL allowed we must NOT set a negative button (would crash).
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Secure Folder")
            .setSubtitle("Confirm it's you to view secured photos")
            .setAllowedAuthenticators(allowed)
            .build()
        prompt.authenticate(info)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }
        enableEdgeToEdge()
        var externalItem: com.gorilla.gallery.data.model.MediaItem? = null
        if (intent?.action == android.content.Intent.ACTION_VIEW) {
            val uri = intent?.data
            if (uri != null) {
                val mime = intent?.type ?: contentResolver.getType(uri) ?: ""
                var width = 0
                var height = 0
                var durationMs = 0L
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(this, uri)
                    width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH)?.toIntOrNull() ?: 0
                    height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT)?.toIntOrNull() ?: 0
                    durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    
                    val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    if (rotation == 90 || rotation == 270) {
                        val temp = width
                        width = height
                        height = temp
                    }
                    retriever.release()
                } catch (e: Exception) {}

                var displayName = "External"
                var sizeBytes = 0L
                try {
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: "External"
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                    }
                    cursor?.close()
                } catch (e: Exception) {}

                externalItem = com.gorilla.gallery.data.model.MediaItem(
                    id = -1,
                    uri = uri,
                    type = if (mime.startsWith("video/")) com.gorilla.gallery.data.model.MediaType.VIDEO else com.gorilla.gallery.data.model.MediaType.IMAGE,
                    displayName = displayName,
                    mimeType = mime,
                    dateTakenMs = System.currentTimeMillis(),
                    dateAddedSec = System.currentTimeMillis() / 1000,
                    dateModifiedSec = System.currentTimeMillis() / 1000,
                    sizeBytes = sizeBytes,
                    width = width,
                    height = height,
                    durationMs = durationMs,
                    bucketId = 0,
                    bucketName = "",
                    relativePath = "",
                    orientation = 0
                )
            }
        }
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
            activeAppViewModel = appViewModel
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val dynamic by appViewModel.dynamicColors.collectAsStateWithLifecycle()

            // Launch system consent dialogs for trash / delete / secure / overwrite.
            var pending by remember { mutableStateOf<ConsentRequest?>(null) }
            val consentLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                pending?.let { appViewModel.onConsentResult(it, result.resultCode == RESULT_OK) }
                pending = null
            }
            LaunchedEffect(Unit) {
                appViewModel.pendingConsent.collect { req ->
                    pending = req
                    consentLauncher.launch(IntentSenderRequest.Builder(req.sender).build())
                }
            }

            val isDark = when (settings.themeMode) {
                ThemeMode.AUTO -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED -> true
            }
            LaunchedEffect(isDark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
            appViewModel.updateDarkTheme(isDark)

            val resolvedAccent = settings.accent.resolve(isDark)
            val resolvedDynamic = dynamic.copy(accent = resolvedAccent)

            GorillaTheme(
                themeMode = settings.themeMode,
                accent = resolvedAccent,
                blurIntensity = settings.blurIntensity,
                liquidGlassIntensity = settings.liquidGlassIntensity,
                surfaceOpacity = settings.surfaceOpacity,
                dynamicColors = resolvedDynamic,
            ) {
                CompositionLocalProvider(LocalBiometricGateway provides this@MainActivity) {
                    GalleryRoot(appViewModel, externalItem, onExternalClose = { finish() })
                }
            }
        }
        checkForUpdates()
    }

    private fun checkForUpdates() {
        val prefs = getSharedPreferences("gorilla_updates", android.content.Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong("last_check_time", 0L)
        val now = System.currentTimeMillis()
        if (now - lastCheck < 24 * 60 * 60 * 1000L) {
            return
        }
        prefs.edit().putLong("last_check_time", now).apply()

        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.github.com/repos/RiderX9/GorillaGallery/releases/latest")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (connection.responseCode == 200) {
                    val json = connection.inputStream.bufferedReader().readText()
                    val obj = org.json.JSONObject(json)
                    val tag = obj.getString("tag_name").removePrefix("v").removePrefix("V")
                    val htmlUrl = obj.getString("html_url")
                    val current = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V")
                    if (com.gorilla.gallery.utils.VersionUtils.isNewerVersion(current, tag)) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(htmlUrl))
                        val pendingIntent = android.app.PendingIntent.getActivity(this@MainActivity, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)
                        
                        val channelId = "updates"
                        val manager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = android.app.NotificationChannel(channelId, "Updates", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                            manager.createNotificationChannel(channel)
                        }
                        
                        val notification = androidx.core.app.NotificationCompat.Builder(this@MainActivity, channelId)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Gorilla Gallery Update")
                            .setContentText("Version $tag is available. Tap to download.")
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()
                            
                        manager.notify(1001, notification)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
