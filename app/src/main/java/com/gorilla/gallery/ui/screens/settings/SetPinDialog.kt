package com.gorilla.gallery.ui.screens.settings

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalDynamicColors

/** Set / change the Secure Folder PIN (4–8 digits, confirmed twice). */
@Composable
fun SetPinDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length in 4..8 && pin == confirm
    val accent = LocalDynamicColors.current.accent

    com.gorilla.gallery.ui.components.AnimatedGlassDialog(onDismissRequest = onDismiss) { scale ->
        LiquidGlassSurface(
            modifier = Modifier.padding(24.dp).scale(scale),
            shape = RoundedCornerShape(DesignTokens.RadiusCard)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Set Secure Folder PIN",
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignTokens.TextPrimary
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN (4–8 digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.padding(top = 12.dp)
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirm = it },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (pin.isNotEmpty() && confirm.isNotEmpty() && pin != confirm) {
                    Text(
                        text = "PINs don't match",
                        modifier = Modifier.padding(top = 6.dp),
                        color = Color.Red
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = DesignTokens.TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(enabled = valid, onClick = { onConfirm(pin) }) {
                        Text(
                            text = "Save",
                            color = if (valid) accent else DesignTokens.TextSecondary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
