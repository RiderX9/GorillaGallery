package com.gorilla.gallery.ui.nav

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private val NavItemWidth = 56.dp
private val NavItemSpacing = 8.dp
private val NavHorizontalPadding = 20.dp
private val NavVerticalPadding = 10.dp
private val NavIndicatorHeight = 40.dp

@Composable
fun GlassNavigationBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassNavigationBar(
        current = current,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
private fun LiquidGlassNavigationBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentBackdrop = LocalLiquidGlassContentBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    var lastValidIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val selectedIndex = remember(current) {
        val idx = Destination.bottomBar.indexOf(current)
        if (idx >= 0) {
            lastValidIndex = idx
            idx
        } else {
            lastValidIndex
        }
    }
    val haptic = rememberHaptic()

    FloatingBottomBar(
        modifier = modifier,
        selectedIndex = selectedIndex,
        onSelected = { index -> 
            val dest = Destination.bottomBar.getOrNull(index)
            if (dest != null && dest != current) {
                haptic()
                onSelect(dest)
            }
        },
        backdrop = contentBackdrop,
        tabsCount = Destination.bottomBar.size,
        isBlurEnabled = true,
        showHighlight = Destination.bottomBar.indexOf(current) != -1
    ) {
        Destination.bottomBar.forEach { dest ->
            val isSelected = dest == current
            val accent = LocalDynamicColors.current.accent
            val inactiveColor = DesignTokens.TextSecondary
            val tint = if (isSelected) accent.copy(alpha = 0.75f) else inactiveColor

            FloatingBottomBarItem(
                onClick = {
                    if (!isSelected) {
                        haptic()
                        onSelect(dest)
                    }
                },
                modifier = Modifier.defaultMinSize(minWidth = 76.dp)
            ) {
                Icon(
                    imageVector = dest.icon,
                    contentDescription = dest.label,
                    modifier = Modifier.size(24.dp),
                    tint = tint
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dest.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalDynamicColors.current.accent
    val inactiveColor = DesignTokens.TextSecondary
    val tint = if (selected) accent.copy(alpha = 0.75f) else inactiveColor
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .pressScale(interaction)
            .clickable(interaction, indication = null) {
                if (!selected) haptic()
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = NavItemWidth, height = NavIndicatorHeight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(24.dp),
                tint = tint
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun GlassNavigationRail(
    current: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()
    val contentBackdrop = LocalLiquidGlassContentBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    
    val destinations = listOf(Destination.Timeline, Destination.Albums, Destination.Settings, Destination.Search)
    var lastValidIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val selectedIndex = remember(current) {
        val idx = destinations.indexOf(current)
        if (idx >= 0) {
            lastValidIndex = idx
            idx
        } else {
            lastValidIndex
        }
    }

    FloatingSideBar(
        modifier = modifier,
        selectedIndex = selectedIndex,
        onSelected = { index -> 
            val dest = destinations.getOrNull(index)
            if (dest != null && dest != current) {
                haptic()
                onSelect(dest)
            }
        },
        backdrop = contentBackdrop,
        tabsCount = destinations.size,
        isBlurEnabled = true,
        showHighlight = destinations.indexOf(current) != -1
    ) {
        destinations.forEach { dest ->
            val isSelected = dest == current
            val accent = LocalDynamicColors.current.accent
            val inactiveColor = DesignTokens.TextSecondary
            val tint = if (isSelected) accent.copy(alpha = 0.75f) else inactiveColor

            FloatingSideBarItem(
                onClick = {
                    if (!isSelected) {
                        haptic()
                        onSelect(dest)
                    }
                },
                modifier = Modifier.defaultMinSize(minHeight = 68.dp)
            ) {
                Icon(
                    imageVector = dest.icon,
                    contentDescription = dest.label,
                    modifier = Modifier.size(24.dp),
                    tint = tint
                )
            }
        }
    }
}
