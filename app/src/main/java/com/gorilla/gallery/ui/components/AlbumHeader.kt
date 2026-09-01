package com.gorilla.gallery.ui.components

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.pressScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.List
import androidx.compose.ui.res.vectorResource

import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@Composable
fun AlbumHeader(
    title: String,
    eyebrow: String,
    countText: String,
    onBack: (() -> Unit)?,
    onSelectToggle: (() -> Unit)? = null,
    customActionText: String? = null,
    onCustomAction: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    headerActions: @Composable (() -> Unit)? = null,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null,
    onRenameAlbum: ((String) -> Unit)? = null,
    onShareAlbum: (() -> Unit)? = null,
    onExportAlbum: (() -> Unit)? = null,
    sortMode: String = "Date Taken",
    onSortModeChange: ((String) -> Unit)? = null,
    sortAscending: Boolean = false,
    onSortAscendingChange: ((Boolean) -> Unit)? = null,
    filters: List<String> = emptyList(),
    selectedFilter: String = "",
    onFilterSelected: (String) -> Unit = {},
    backdrop: com.kyant.backdrop.Backdrop?,
    modifier: Modifier = Modifier,
    showMoreMenu: Boolean = true
) {
    val appColors = LocalAppColors.current
    val accentColor = com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent
    
    // Fallback colors for dark/light mode matching HTML design
    val successColor = Color(0xFF34d399) 
    
    var isSearchActive by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(appColors.bgBase)
            .statusBarsPadding()
            .padding(bottom = 0.dp)
    ) {
        // Unified Top Floating Island
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Navigation Pill
            Row(
                modifier = Modifier
                    .let { if (onBack != null) it.pressScale(0.96f) else it }
                    .height(42.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(21.dp))
                    .background(appColors.bgSurface.copy(alpha = 0.5f))
                    .border(1.dp, appColors.borderGlass, androidx.compose.foundation.shape.RoundedCornerShape(21.dp))
                    .let { if (onBack != null) it.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = { onBack() }) else it }
                    .padding(start = 5.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(appColors.textPrimary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = appColors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(Modifier.width(8.dp))
                }
                
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (countText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = countText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 11.sp,
                            color = appColors.textSecondary
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .height(42.dp)
                    .background(appColors.bgSurface.copy(alpha = 0.9f), RoundedCornerShape(21.dp))
                    .border(1.dp, appColors.borderGlass, RoundedCornerShape(21.dp))
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                if (onCustomAction != null && customActionText != null) {
                    Box(
                        modifier = Modifier
                            .pressScale(0.94f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onCustomAction() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customActionText,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.01.sp
                        )
                    }
                } else if (onSelectToggle != null) {
                    Box(
                        modifier = Modifier
                            .pressScale(0.94f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onSelectToggle() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSelectionMode) "Cancel" else "Select",
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.01.sp
                        )
                    }
                }
                
                val hasStartAction = (onCustomAction != null && customActionText != null) || (onSelectToggle != null)
                Box(
                    modifier = Modifier
                        .pressScale(0.94f)
                        .size(36.dp)
                        .clip(if (hasStartAction) RoundedCornerShape(6.dp) else RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                        .background(appColors.textPrimary.copy(alpha = 0.08f))
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { showSortDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = com.gorilla.gallery.R.drawable.ic_sort_custom),
                        contentDescription = "Sort Options",
                        tint = appColors.textPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                }
                
                if (headerActions != null) {
                    headerActions()
                }

                if (showMoreMenu) {
                    Box(
                        modifier = Modifier
                            .pressScale(0.94f)
                            .size(36.dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                            .background(appColors.textPrimary.copy(alpha = 0.08f))
                            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { isMenuOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "More Options",
                            tint = appColors.textPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        
                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false },
                            modifier = Modifier.background(appColors.bgSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select All", color = appColors.textPrimary) },
                                onClick = { 
                                    isMenuOpen = false
                                    onSelectAll?.invoke()
                                }
                            )
                            if (onRenameAlbum != null) {
                                DropdownMenuItem(
                                    text = { Text("Rename", color = appColors.textPrimary) },
                                    onClick = { 
                                        isMenuOpen = false
                                        showRenameDialog = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Share Album", color = appColors.textPrimary) },
                                onClick = { 
                                    isMenuOpen = false
                                    onShareAlbum?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Zip/PDF", color = appColors.textPrimary) },
                                onClick = { 
                                    isMenuOpen = false
                                    onExportAlbum?.invoke()
                                }
                            )
                        }
                    }
                }
            }
        }
        
        val context = androidx.compose.ui.platform.LocalContext.current
        if (showRenameDialog) {
            var newName by remember { mutableStateOf(title) }
            val canCreate = newName.isNotBlank()
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showRenameDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = { showRenameDialog = false }),
                    contentAlignment = Alignment.Center,
                ) {
                    com.gorilla.gallery.ui.theme.LiquidGlassSurface(
                        depth = com.gorilla.gallery.ui.theme.GlassDepth.MID,
                        shape = RoundedCornerShape(28.dp),
                        backdrop = backdrop ?: com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop.current ?: com.gorilla.gallery.ui.theme.LocalLiquidGlassBackdrop.current,
                        surfaceColor = appColors.bgSurface.copy(alpha = 0.92f),
                        saturationOverride = 1.55f, tintAlphaOverride = 0.07f,
                        modifier = Modifier.width(300.dp).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = {}),
                    ) {
                        Column(modifier = Modifier.padding(28.dp)) {
                            Text(text = "Rename", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = com.gorilla.gallery.ui.theme.DesignTokens.TextPrimary, modifier = Modifier.fillMaxWidth())
                            Box(Modifier.height(18.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = newName, onValueChange = { newName = it }, singleLine = true,
                                label = { Text("Name") }, modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor, unfocusedBorderColor = appColors.textPrimary.copy(alpha = 0.2f),
                                    focusedContainerColor = appColors.textPrimary.copy(alpha = 0.1f), unfocusedContainerColor = appColors.textPrimary.copy(alpha = 0.05f)
                                )
                            )
                            Box(Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.TextButton(onClick = { showRenameDialog = false }, modifier = Modifier.pressScale(scale = 0.94f)) {
                                    Text(text = "Cancel", style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary)
                                }
                                Box(Modifier.width(8.dp))
                                androidx.compose.material3.TextButton(enabled = canCreate, onClick = { showRenameDialog = false; android.widget.Toast.makeText(context, "Renamed to $newName", android.widget.Toast.LENGTH_SHORT).show(); onRenameAlbum?.invoke(newName) }, modifier = Modifier.pressScale(scale = 0.94f)) {
                                    Text(text = "Save", style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = if (canCreate) accentColor else com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary.copy(alpha = 0.42f))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSortDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showSortDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = { showSortDialog = false }),
                    contentAlignment = Alignment.Center,
                ) {
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        
                        com.gorilla.gallery.ui.theme.LiquidGlassSurface(
                            depth = com.gorilla.gallery.ui.theme.GlassDepth.MID, shape = RoundedCornerShape(28.dp),
                            backdrop = backdrop ?: com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop.current ?: com.gorilla.gallery.ui.theme.LocalLiquidGlassBackdrop.current,
                            surfaceColor = appColors.bgSurface.copy(alpha = 0.92f),
                            saturationOverride = 1.55f, tintAlphaOverride = 0.07f,
                            modifier = Modifier.widthIn(min = 300.dp, max = if (isLandscape) 560.dp else 300.dp).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = {}),
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(text = "Sort & Order Options", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = com.gorilla.gallery.ui.theme.DesignTokens.TextPrimary, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp))
                                
                                val haptic = com.gorilla.gallery.ui.theme.rememberHaptic()
                                @Composable fun SortOption(label: String, selected: Boolean, onClick: () -> Unit) {
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) accentColor.copy(alpha=0.15f) else Color.Transparent).clickable { haptic(); onClick(); showSortDialog = false }.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = label, fontSize = 17.sp, color = if (selected) accentColor else appColors.textPrimary, modifier = Modifier.weight(1f))
                                    }
                                }
                                @Composable fun OrderOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) accentColor.copy(alpha=0.15f) else Color.Transparent).clickable { haptic(); onClick(); showSortDialog = false }.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = label, fontSize = 17.sp, color = if (selected) accentColor else appColors.textPrimary, modifier = Modifier.weight(1f))
                                        androidx.compose.material3.Icon(imageVector = icon, contentDescription = label, tint = if (selected) accentColor else appColors.textPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }

                                if (isLandscape) {
                                    Row(modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sort By", color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                            SortOption("Date Taken", sortMode == "Date Taken") { onSortModeChange?.invoke("Date Taken") }
                                            SortOption("Date Modified", sortMode == "Date Modified") { onSortModeChange?.invoke("Date Modified") }
                                            SortOption("Name (A-Z)", sortMode == "Name (A-Z)") { onSortModeChange?.invoke("Name (A-Z)") }
                                            SortOption("File Size", sortMode == "File Size") { onSortModeChange?.invoke("File Size") }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            Text("Order", color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                            OrderOption("Ascending", Icons.Rounded.ArrowUpward, sortAscending) { onSortAscendingChange?.invoke(true) }
                                            OrderOption("Descending", Icons.Rounded.ArrowDownward, !sortAscending) { onSortAscendingChange?.invoke(false) }
                                            
                                            Spacer(modifier = Modifier.weight(1f))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                androidx.compose.material3.TextButton(onClick = { showSortDialog = false }, modifier = Modifier.pressScale(scale = 0.94f)) {
                                                    Text(text = "Close", style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = accentColor)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text("Sort By", color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    SortOption("Date Taken", sortMode == "Date Taken") { onSortModeChange?.invoke("Date Taken") }
                                    SortOption("Date Modified", sortMode == "Date Modified") { onSortModeChange?.invoke("Date Modified") }
                                    SortOption("Name (A-Z)", sortMode == "Name (A-Z)") { onSortModeChange?.invoke("Name (A-Z)") }
                                    SortOption("File Size", sortMode == "File Size") { onSortModeChange?.invoke("File Size") }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Order", color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    OrderOption("Ascending", Icons.Rounded.ArrowUpward, sortAscending) { onSortAscendingChange?.invoke(true) }
                                    OrderOption("Descending", Icons.Rounded.ArrowDownward, !sortAscending) { onSortAscendingChange?.invoke(false) }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        androidx.compose.material3.TextButton(onClick = { showSortDialog = false }, modifier = Modifier.pressScale(scale = 0.94f)) {
                                            Text(text = "Close", style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = accentColor)
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
        

        // Filter Strip
        if (filters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    val isActive = filter == selectedFilter
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pressScale(0.95f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) accentColor.copy(alpha = 0.16f) else appColors.bgSurface.copy(alpha = 0.85f))
                            .border(1.dp, if (isActive) accentColor.copy(alpha = 0.35f) else appColors.borderGlass, RoundedCornerShape(12.dp))
                            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onFilterSelected(filter) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (isActive) accentColor else appColors.textPrimary.copy(alpha = 0.55f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

