package com.gorilla.gallery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.ui.theme.kernelSuGlassBackdrop
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic

import com.kyant.backdrop.Backdrop

/**
 * Compact glass action bar shown in selection mode: a bare count number followed by the
 * Share / Favorite / Secure / Delete icons, all on one vertically-centered row. The pill
 * hugs its content (no fixed width) so nothing can be truncated.
 */
@Composable
fun SelectionActionBar(
    count: Int,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onSecure: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    val surfaceOpacity = com.gorilla.gallery.ui.theme.LocalTrueLiquidGlassSurfaceOpacity.current
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .wrapContentWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
            .kernelSuGlassBackdrop(
                backdrop = backdrop ?: LocalLiquidGlassContentBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop(),
                shape = RoundedCornerShape(percent = 50),
                surfaceAlphaOverride = surfaceOpacity
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = LocalDynamicColors.current.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(44.dp),
            )
            Spacer(Modifier.width(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIcon(Icons.Rounded.Share, "Share", onShare)
                ActionIcon(Icons.Rounded.Favorite, "Favorite", onFavorite)
                ActionIcon(Icons.Rounded.Lock, "Lock", onSecure)
                ActionIcon(Icons.Rounded.Delete, "Delete", onDelete)
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    Icon(
        icon,
        contentDescription = label,
        tint = LocalAppColors.current.textPrimary,
        modifier = Modifier
            .pressScale(interaction)
            .clickable(interaction, indication = null) { haptic(); onClick() }
            .size(26.dp),
    )
}
