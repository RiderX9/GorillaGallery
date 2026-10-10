package com.gorilla.gallery.ui.components

import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.ui.theme.kernelSuGlassBackdrop
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic

import com.kyant.backdrop.Backdrop

@Composable
fun SelectionActionBar(
    count: Int,
    allSelected: Boolean = false,
    onSelectAll: () -> Unit = {},
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
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$count Selected",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalAppColors.current.textPrimary
                )
                val interaction = remember { MutableInteractionSource() }
                Text(
                    text = if (allSelected) "Deselect All" else "Select All",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalDynamicColors.current.accent,
                    modifier = Modifier
                        .pressScale(interaction)
                        .clickable(interactionSource = interaction, indication = null) { onSelectAll() }
                        .padding(top = 2.dp)
                )
            }
            Spacer(Modifier.width(28.dp))
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
    tint: Color = Color.Unspecified
) {
    val actualTint = if (tint == Color.Unspecified) LocalAppColors.current.textPrimary else tint
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    Icon(
        icon,
        contentDescription = label,
        tint = actualTint,
        modifier = Modifier
            .pressScale(interaction)
            .clickable(interaction, indication = null) { haptic(); onClick() }
            .size(26.dp),
    )
}

@Composable
fun InternalSelectionActionBar(
    count: Int,
    allSelected: Boolean = false,
    onSelectAll: () -> Unit = {},
    onShare: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    restoreLabel: String = "Restore",
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
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$count Selected",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalAppColors.current.textPrimary
                )
                val interaction = remember { MutableInteractionSource() }
                Text(
                    text = if (allSelected) "Deselect All" else "Select All",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalDynamicColors.current.accent,
                    modifier = Modifier
                        .pressScale(interaction)
                        .clickable(interactionSource = interaction, indication = null) { onSelectAll() }
                        .padding(top = 2.dp)
                )
            }
            Spacer(Modifier.width(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIcon(Icons.Rounded.Share, "Share", onShare)
                ActionIcon(Icons.Rounded.RestoreFromTrash, restoreLabel, onRestore)
                ActionIcon(Icons.Rounded.Delete, "Delete", onDelete)
            }
        }
    }
}
