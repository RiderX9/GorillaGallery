package com.gorilla.gallery.ui.screens.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.GlassDepth
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic

@Composable
fun PermissionGate(onRequest: () -> Unit) {
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignTokens.BgBase)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        HeroArtwork(accent = accent)

        Spacer(modifier = Modifier.height(24.dp))

        HeaderGroup(accent = accent)

        Spacer(modifier = Modifier.height(32.dp))

        PermissionsContainer(accent = accent)

        Spacer(modifier = Modifier.weight(1.2f))

        ActionDeck(accent = accent, haptic = haptic, onRequest = onRequest)
    }
}

@Composable
private fun HeroArtwork(accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left Card
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(115.dp)
                .graphicsLayer {
                    rotationZ = -14f
                    translationX = -42.dp.toPx()
                    translationY = 8.dp.toPx()
                    alpha = 0.75f
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = 0.15f), DesignTokens.BgSurface)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
        )

        // Right Card
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(115.dp)
                .graphicsLayer {
                    rotationZ = 15f
                    translationX = 42.dp.toPx()
                    translationY = 6.dp.toPx()
                    alpha = 0.75f
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = 0.1f), DesignTokens.BgSurface)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
        )

        // Center Card
        Box(
            modifier = Modifier
                .width(106.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = 0.35f), DesignTokens.BgBase)
                    )
                )
                .border(1.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.gorilla.gallery.R.drawable.welcome_middle),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().scale(1.35f),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}

@Composable
private fun HeaderGroup(accent: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Text(
            text = "GORILLA GALLERY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                fontSize = 11.sp
            ),
            color = accent
        )
        Text(
            text = "Your Library,\nCompletely Private.",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                lineHeight = 30.sp
            ),
            color = DesignTokens.TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "To organize, edit, and categorize your photos on-device without cloud tracking.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            color = DesignTokens.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
private fun PermissionsContainer(accent: Color) {
    LiquidGlassSurface(
        depth = GlassDepth.HIGH,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
    ) {
        Column {
            PermissionRow(
                icon = Icons.Rounded.PhotoLibrary,
                title = "Local Media Access",
                desc = "Required to seamlessly load, display, and organize your local photos and videos.",
                tint = accent
            )
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            PermissionRow(
                icon = Icons.Rounded.Lock,
                title = "100% Offline Privacy",
                desc = "Your library never leaves this phone. No cloud sync, no tracking, total privacy.",
                tint = Color(0xFF38BDF8) // Light blue
            )
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            PermissionRow(
                icon = Icons.Rounded.Search,
                title = "Powerful yet Simple",
                desc = "Edit your photos and videos, effortlessly organize albums, and explore using Google Lens.",
                tint = Color(0xFFFBBF24) // Amber
            )
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, desc: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title, 
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp), 
                color = DesignTokens.TextPrimary
            )
            Text(
                desc, 
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp), 
                color = DesignTokens.TextSecondary
            )
        }
    }
}

@Composable
private fun ActionDeck(accent: Color, haptic: () -> Unit, onRequest: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        var isSettingUp by remember { mutableStateOf(false) }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pressScale(interactionSource)
                .clip(RoundedCornerShape(26.dp))
                .background(accent)
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
                .clickable(interactionSource, indication = null) {
                    if (!isSettingUp) {
                        haptic()
                        isSettingUp = true
                        onRequest()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isSettingUp) "Setting up library..." else "Grant Access",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = Color(0xFF0B0A12)
                )
                if (!isSettingUp) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF0B0A12)
                    )
                }
            }
        }
        
        Text(
            text = "Permissions can be modified anytime in your device's Settings.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = DesignTokens.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
