package com.gorilla.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.ColorUtils

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    var hsv by remember { 
        val hsvArray = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArray)
        mutableStateOf(hsvArray) 
    }
    
    var currentColor by remember { mutableStateOf(initialColor) }

    // Update currentColor when hsv changes
    LaunchedEffect(hsv) {
        currentColor = Color(android.graphics.Color.HSVToColor(hsv))
    }

    GlassCustomDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.wrapContentSize()
    ) {
        val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                ) {
                    // SV Area (Saturation/Value)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.White, Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))))
                                )
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black)
                                )
                            )
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val position = change.position
                                    val s = (position.x / size.width).coerceIn(0f, 1f)
                                    val v = 1f - (position.y / size.height).coerceIn(0f, 1f)
                                    hsv = floatArrayOf(hsv[0], s, v)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                                    hsv = floatArrayOf(hsv[0], s, v)
                                }
                            }
                    ) {
                        // Indicator
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (hsv[1] * 200).dp - 8.dp,
                                    y = ((1f - hsv[2]) * 200).dp - 8.dp
                                )
                                .size(16.dp)
                                .border(2.dp, Color.White, CircleShape)
                                .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max).height(200.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pick a Color",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green).forEach { presetColor ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(presetColor)
                                            .then(
                                                if (android.graphics.Color.HSVToColor(hsv) == presetColor.toArgb()) {
                                                    androidx.compose.ui.Modifier.border(2.dp, Color.White, CircleShape)
                                                } else {
                                                    androidx.compose.ui.Modifier
                                                }
                                            )
                                            .clickable {
                                                val newHsv = FloatArray(3)
                                                android.graphics.Color.colorToHSV(presetColor.toArgb(), newHsv)
                                                hsv = newHsv
                                            }
                                    )
                                }
                            }
                        }

                        // Hue Slider
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 1f, 1f))),
                                            Color(android.graphics.Color.HSVToColor(floatArrayOf(60f, 1f, 1f))),
                                            Color(android.graphics.Color.HSVToColor(floatArrayOf(120f, 1f, 1f))),
                                            Color(android.graphics.Color.HSVToColor(floatArrayOf(180f, 1f, 1f))),
                                            Color(android.graphics.Color.HSVToColor(floatArrayOf(240f, 1f, 1f))),
                                            Color(android.graphics.Color.HSVToColor(floatArrayOf(300f, 1f, 1f))),
                                            Color(android.graphics.Color.HSVToColor(floatArrayOf(360f, 1f, 1f)))
                                        )
                                    )
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val hue = ((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                                        hsv = floatArrayOf(hue, hsv[1], hsv[2])
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val hue = ((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                                        hsv = floatArrayOf(hue, hsv[1], hsv[2])
                                    }
                                }
                        ) {
                            // Indicator
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = ((hsv[0] / 360f) * 200).dp - 4.dp,
                                    )
                                    .width(8.dp)
                                    .fillMaxHeight()
                                    .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                                    .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(currentColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DialogPillButton(
                                    text = "Cancel",
                                    onClick = onDismissRequest,
                                    textColor = Color.White,
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                )
                                DialogPillButton(
                                    text = "OK",
                                    isPrimary = true,
                                    onClick = { 
                                        onColorSelected(currentColor)
                                        onDismissRequest()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(IntrinsicSize.Min)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pick a Color",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green).forEach { presetColor ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(presetColor)
                                    .then(
                                        if (android.graphics.Color.HSVToColor(hsv) == presetColor.toArgb()) {
                                            androidx.compose.ui.Modifier.border(2.dp, Color.White, CircleShape)
                                        } else {
                                            androidx.compose.ui.Modifier
                                        }
                                    )
                                    .clickable {
                                        val newHsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(presetColor.toArgb(), newHsv)
                                        hsv = newHsv
                                    }
                            )
                        }
                    }
                }

                // SV Area (Saturation/Value)
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.White, Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))))
                            )
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val position = change.position
                                val s = (position.x / size.width).coerceIn(0f, 1f)
                                val v = 1f - (position.y / size.height).coerceIn(0f, 1f)
                                hsv = floatArrayOf(hsv[0], s, v)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val s = (offset.x / size.width).coerceIn(0f, 1f)
                                val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                                hsv = floatArrayOf(hsv[0], s, v)
                            }
                        }
                ) {
                    // Indicator
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (hsv[1] * 240).dp - 8.dp,
                                y = ((1f - hsv[2]) * 240).dp - 8.dp
                            )
                            .size(16.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hue Slider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 1f, 1f))),
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(60f, 1f, 1f))),
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(120f, 1f, 1f))),
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(180f, 1f, 1f))),
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(240f, 1f, 1f))),
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(300f, 1f, 1f))),
                                    Color(android.graphics.Color.HSVToColor(floatArrayOf(360f, 1f, 1f)))
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val hue = ((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                                hsv = floatArrayOf(hue, hsv[1], hsv[2])
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val hue = ((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                                hsv = floatArrayOf(hue, hsv[1], hsv[2])
                            }
                        }
                ) {
                    // Indicator
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((hsv[0] / 360f) * 240).dp - 4.dp,
                            )
                            .width(8.dp)
                            .fillMaxHeight()
                            .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                            .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DialogPillButton(
                            text = "Cancel",
                            onClick = onDismissRequest,
                            textColor = Color.White,
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        )
                        DialogPillButton(
                            text = "OK",
                            isPrimary = true,
                            onClick = { 
                                onColorSelected(currentColor)
                                onDismissRequest()
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        )
                    }
                }
            }
        }
    }
}
