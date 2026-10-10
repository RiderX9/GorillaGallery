package com.gorilla.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.GlassDepth
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.LocalLiquidGlassBackdrop
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.gallery.ui.theme.rememberHaptic
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.geometry.Offset

@Composable
fun SortAndOrderDialog(
    sortMode: String,
    sortAscending: Boolean,
    onDismissRequest: () -> Unit,
    onSortModeChange: (String) -> Unit,
    onSortAscendingChange: (Boolean) -> Unit,
    backdrop: com.kyant.backdrop.Backdrop? = null
) {
    AnimatedGlassDialog(onDismissRequest = onDismissRequest) { scale ->
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val appColors = LocalAppColors.current
        val accentColor = LocalDynamicColors.current.accent
        val haptic = rememberHaptic()

        var hoveredIndex by remember { mutableIntStateOf(-1) }
        var activeGroup by remember { mutableIntStateOf(-1) }
        val itemBounds = remember { mutableMapOf<Int, Rect>() } // Window coordinates
        var surfaceCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        
        val getGroup = { index: Int -> if (index in 0..3) 0 else if (index in 4..5) 1 else -1 }
        
        val handleSelection = { index: Int ->
            when (index) {
                0 -> { onSortModeChange("Date Taken"); onSortAscendingChange(false) }
                1 -> { onSortModeChange("Date Modified"); onSortAscendingChange(false) }
                2 -> { onSortModeChange("Name (A-Z)"); onSortAscendingChange(true) }
                3 -> { onSortModeChange("File Size"); onSortAscendingChange(false) }
                4 -> onSortAscendingChange(true)
                5 -> onSortAscendingChange(false)
            }
            if (index in 0..5) {
                onDismissRequest()
            }
        }

        LiquidGlassSurface(
            depth = GlassDepth.MID,
            shape = RoundedCornerShape(28.dp),
            backdrop = backdrop ?: LocalLiquidGlassContentBackdrop.current ?: LocalLiquidGlassBackdrop.current,
            surfaceColor = appColors.bgSurface.copy(alpha = 0.92f),
            saturationOverride = 1.55f,
            tintAlphaOverride = 0.07f,
            modifier = Modifier
                .widthIn(min = 300.dp, max = if (isLandscape) 560.dp else 300.dp)
                .scale(scale)
                .onGloballyPositioned { surfaceCoords = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.first()
                            
                            val surfaceC = surfaceCoords
                            if (surfaceC != null) {
                                val windowPos = surfaceC.localToWindow(change.position)
                                
                                if (!change.previousPressed && change.pressed) {
                                    // Touch Down
                                    var found = -1
                                    for ((idx, bounds) in itemBounds) {
                                        if (bounds.contains(windowPos)) {
                                            found = idx
                                            break
                                        }
                                    }
                                    if (found != -1) {
                                        activeGroup = getGroup(found)
                                        hoveredIndex = found
                                        haptic()
                                    }
                                } else if (change.previousPressed && change.pressed) {
                                    // Touch Move
                                    if (activeGroup != -1) {
                                        var found = -1
                                        for ((idx, bounds) in itemBounds) {
                                            if (bounds.contains(windowPos) && getGroup(idx) == activeGroup) {
                                                found = idx
                                                break
                                            }
                                        }
                                        if (found != hoveredIndex && found != -1) {
                                            haptic()
                                            hoveredIndex = found
                                        }
                                    }
                                } else if (change.previousPressed && !change.pressed) {
                                    // Touch Up
                                    if (hoveredIndex != -1) {
                                        handleSelection(hoveredIndex)
                                    }
                                    hoveredIndex = -1
                                    activeGroup = -1
                                }
                            }
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Sort & Order Options",
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignTokens.TextPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
                )

                @Composable
                fun OptionItem(index: Int, label: String, icon: ImageVector? = null, selected: Boolean) {
                    val isHovered = hoveredIndex == index
                    val isDraggingInMyGroup = hoveredIndex != -1 && getGroup(hoveredIndex) == getGroup(index)
                    val isActive = isHovered || (selected && !isDraggingInMyGroup)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { itemBounds[index] = it.boundsInWindow() }
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isActive) accentColor.copy(alpha = if (isHovered) 0.25f else 0.15f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 17.sp,
                            color = if (isActive) accentColor else appColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isActive) accentColor else appColors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (isLandscape) {
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sort By", color = DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            OptionItem(0, "Date Taken", selected = sortMode == "Date Taken")
                            OptionItem(1, "Date Modified", selected = sortMode == "Date Modified")
                            OptionItem(2, if (sortMode == "Name (A-Z)" && !sortAscending) "Name (Z-A)" else "Name (A-Z)", selected = sortMode == "Name (A-Z)")
                            OptionItem(3, "File Size", selected = sortMode == "File Size")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Text("Order", color = DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            OptionItem(4, "Ascending", Icons.Rounded.ArrowUpward, selected = sortAscending)
                            OptionItem(5, "Descending", Icons.Rounded.ArrowDownward, selected = !sortAscending)

                            Spacer(modifier = Modifier.weight(1f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                DialogPillButton(
                                    text = "Close",
                                    onClick = onDismissRequest
                                )
                            }
                        }
                    }
                } else {
                    Text("Sort By", color = DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    OptionItem(0, "Date Taken", selected = sortMode == "Date Taken")
                    OptionItem(1, "Date Modified", selected = sortMode == "Date Modified")
                    OptionItem(2, if (sortMode == "Name (A-Z)" && !sortAscending) "Name (Z-A)" else "Name (A-Z)", selected = sortMode == "Name (A-Z)")
                    OptionItem(3, "File Size", selected = sortMode == "File Size")

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Order", color = DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    OptionItem(4, "Ascending", Icons.Rounded.ArrowUpward, selected = sortAscending)
                    OptionItem(5, "Descending", Icons.Rounded.ArrowDownward, selected = !sortAscending)

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        DialogPillButton(
                            text = "Close",
                            onClick = onDismissRequest
                        )
                    }
                }
            }
        }
    }
}
