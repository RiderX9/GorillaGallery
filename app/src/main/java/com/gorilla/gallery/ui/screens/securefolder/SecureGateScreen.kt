package com.gorilla.gallery.ui.screens.securefolder

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.LocalBiometricGateway
import com.gorilla.gallery.ui.theme.CapsuleShape
import com.gorilla.gallery.ui.theme.GlassDepth
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.pressScale

/** Biometric + PIN gate shown until the Secure Folder is unlocked. */
@Composable
fun SecureGateScreen(app: AppViewModel) {
    val gateway = LocalBiometricGateway.current
    val settings by app.settings.collectAsStateWithLifecycle()
    val accent = LocalDynamicColors.current.accent
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val canBiometric = settings.biometricUnlock && gateway?.canAuthenticate() == true



    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(56.dp))
            Text(
                "Secure Folder",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = LocalAppColors.current.textPrimary,
            )
            Text(
                "Unlock to view your secured photos.",
                color = LocalAppColors.current.textSecondary,
                textAlign = TextAlign.Center,
            )

            if (canBiometric) {
                GlassButton(text = "Use biometrics", icon = true) {
                    gateway?.authenticate { ok -> if (ok) app.unlockSecure() }
                }
            }

            if (settings.hasPin) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8 && it.all(Char::isDigit)) { pin = it; error = false }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                if (error) Text("Incorrect PIN", color = androidx.compose.ui.graphics.Color(0xFFFF6B6B))
                GlassButton(text = "Unlock", icon = false) {
                    if (app.verifyPin(pin)) app.unlockSecure() else { error = true; pin = "" }
                }
            }
        }
    }
}

@Composable
private fun GlassButton(text: String, icon: Boolean, onClick: () -> Unit) {
    val accent = LocalDynamicColors.current.accent
    LiquidGlassSurface(
        depth = GlassDepth.MID,
        shape = CapsuleShape,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier
                .androidxClickable(onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon) Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = accent, modifier = Modifier.padding(end = 8.dp))
            Text(text, color = accent, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this
        .pressScale(interaction, pressedScale = 0.94f)
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
}
