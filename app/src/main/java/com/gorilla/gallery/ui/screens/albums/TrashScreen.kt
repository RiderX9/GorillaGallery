package com.gorilla.gallery.ui.screens.albums

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RestoreFromTrash
import com.gorilla.gallery.ui.components.GlassAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.kyant.backdrop.backdrops.layerBackdrop
import com.gorilla.gallery.ui.theme.pressScale
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.EmptyState
import com.gorilla.gallery.ui.components.MediaGrid
import com.gorilla.gallery.ui.components.MediaSection
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Full-screen Trash overlay: restore individual items or empty everything. */
@Composable
fun TrashScreen(
    app: AppViewModel,
    items: List<MediaItem>,
    onBack: () -> Unit,
    viewerOpen: Boolean = false,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
) {
    var confirmEmpty by remember { mutableStateOf(false) }
    var restoreTarget by remember { mutableStateOf<MediaItem?>(null) }
    
    val settings by app.settings.collectAsStateWithLifecycle()
    val focusedItem by app.focusedItem.collectAsStateWithLifecycle()
    var gridColumns by remember { mutableStateOf(settings.gridColumns) }

    var selectedFilter by remember { mutableStateOf("All Media") }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Date Taken") }
    var sortAscending by remember { mutableStateOf(false) }

    val filteredItems = remember(items, selectedFilter, searchQuery, sortMode, sortAscending) {
        val filtered = if (selectedFilter == "All") items else items.filter {
            when (selectedFilter) {
                "Photos" -> !it.isVideo
                "Videos" -> it.isVideo
                else -> true
            }
        }
        val searched = if (searchQuery.isBlank()) filtered else filtered.filter {
            it.displayName.contains(searchQuery, ignoreCase = true)
        }
        val sorted = when (sortMode) {
            "Date Taken" -> searched.sortedBy { it.dateTakenMs }
            "Date Modified" -> searched.sortedBy { it.dateModifiedSec }
            "Name (A-Z)" -> searched.sortedBy { it.displayName }
            "File Size" -> searched.sortedBy { it.sizeBytes }
            else -> searched.sortedBy { it.dateTakenMs }
        }
        if (sortAscending) sorted else sorted.reversed()
    }
    
    var headerHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val topPadding = with(androidx.compose.ui.platform.LocalDensity.current) { headerHeightPx.toDp() }
    val gridBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    androidx.activity.compose.BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(com.gorilla.gallery.ui.theme.DesignTokens.BgBase)) {
        Box(Modifier.layerBackdrop(gridBackdrop)) {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        title = "Trash is empty",
                        subtitle = "Deleted photos stay here for 30 days before they're removed.",
                        icon = Icons.Rounded.DeleteForever,
                    )
                }
            } else {
                com.gorilla.gallery.ui.components.SortTransition(
                    sortState = Pair(sortMode, sortAscending),
                    contentKey = { it }
                ) { state ->
                    val (currentSortMode, currentSortAscending) = state
                    val currentItems = remember(items, selectedFilter, searchQuery, currentSortMode, currentSortAscending) {
                        val filtered = if (selectedFilter == "All") items else items.filter {
                            when (selectedFilter) {
                                "Photos" -> !it.isVideo
                                "Videos" -> it.isVideo
                                else -> true
                            }
                        }
                        val searched = if (searchQuery.isBlank()) filtered else filtered.filter {
                            it.displayName.contains(searchQuery, ignoreCase = true)
                        }
                        val sorted = when (currentSortMode) {
                            "Date Taken" -> searched.sortedBy { it.dateTakenMs }
                            "Date Modified" -> searched.sortedBy { it.dateModifiedSec }
                            "Name (A-Z)" -> searched.sortedBy { it.displayName }
                            "File Size" -> searched.sortedBy { it.sizeBytes }
                            else -> searched.sortedBy { it.dateTakenMs }
                        }
                        if (currentSortAscending) sorted else sorted.reversed()
                    }
                    MediaGrid(
                    viewerOpen = viewerOpen,
                    focusedItem = focusedItem,
                        sections = if (currentItems.isEmpty()) emptyList() else listOf(MediaSection("trash", "", currentItems)),
                        columns = gridColumns,
                        selectionMode = false,
                        selectedIds = emptySet(),
                        highQualityThumbnails = settings.highQualityThumbnails,
                        onColumnsChange = { gridColumns = it },
                        maxColumns = 6,
                        onClick = { item, bounds ->
                            onOpenViewer(currentItems, item.uri, bounds)
                        },
                        onLongClick = { item -> restoreTarget = item },
                        contentPadding = PaddingValues(top = topPadding, bottom = 24.dp),
                        headerContent = null
                    )
                }
            }
        }
            
        Header(
            onBack = onBack, 
            canEmpty = items.isNotEmpty(), 
            onEmpty = { confirmEmpty = true }, 
            count = filteredItems.size, 
            selectedFilter = selectedFilter, 
            onFilterSelected = { selectedFilter = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            sortMode = sortMode,
            onSortModeChange = { sortMode = it },
            sortAscending = sortAscending,
            onSortAscendingChange = { sortAscending = it },
            backdrop = gridBackdrop,
            modifier = Modifier.align(Alignment.TopCenter).onGloballyPositioned { headerHeightPx = it.size.height }
        )
    }

    if (confirmEmpty) {
        GlassAlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = "Empty Trash?",
            text = "All ${items.size} items will be permanently deleted. This cannot be undone.",
            confirmLabel = "Empty",
            onConfirm = { app.emptyTrash(); confirmEmpty = false },
            dismissLabel = "Cancel",
            onDismiss = { confirmEmpty = false }
        )
    }

    restoreTarget?.let { item ->
        GlassAlertDialog(
            onDismissRequest = { restoreTarget = null },
            icon = Icons.Rounded.RestoreFromTrash,
            title = "Restore this item?",
            text = "It will be moved back to ${item.relativePath.ifBlank { "your gallery" }}.",
            confirmLabel = "Restore",
            onConfirm = {
                item.trashId?.let { app.restoreFromTrash(it) }
                restoreTarget = null
            },
            dismissLabel = "Cancel",
            onDismiss = { restoreTarget = null }
        )
    }
}

@Composable
private fun Header(
    onBack: () -> Unit,
    canEmpty: Boolean,
    onEmpty: () -> Unit,
    count: Int,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortMode: String,
    onSortModeChange: (String) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    modifier: Modifier = Modifier
) {
    com.gorilla.gallery.ui.components.AlbumHeader(
        title = "Trash Bin",
        eyebrow = "",
        countText = "$count items",
        onBack = onBack,
        onSelectToggle = null,
        isSelectionMode = false,
        headerActions = {
            val accentColor = com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .pressScale(0.94f)
                    .height(36.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.32f), androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                    .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onEmpty() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.DeleteForever,
                    contentDescription = "Empty",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                androidx.compose.material3.Text(
                    text = "Empty",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 0.01.sp
                )
            }
        },
        filters = listOf("All Media", "Photos", "Videos"),
        selectedFilter = selectedFilter,
        onFilterSelected = onFilterSelected,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        sortMode = sortMode,
        onSortModeChange = onSortModeChange,
        sortAscending = sortAscending,
        onSortAscendingChange = onSortAscendingChange,
        onSelectAll = null,
        onRenameAlbum = null,
        onShareAlbum = null,
        onExportAlbum = null,
        backdrop = backdrop,
        modifier = modifier,
        showMoreMenu = false
    )
}
