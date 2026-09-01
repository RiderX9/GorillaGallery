package com.gorilla.gallery.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Bridge so the Compose Secure Folder gate can trigger BiometricPrompt on the Activity. */
interface BiometricGateway {
    /** True if a strong biometric or device credential can be used right now. */
    fun canAuthenticate(): Boolean
    /** Show the system prompt; [onResult] is true on success, false on cancel/error. */
    fun authenticate(onResult: (Boolean) -> Unit)
}

val LocalBiometricGateway = staticCompositionLocalOf<BiometricGateway?> { null }
