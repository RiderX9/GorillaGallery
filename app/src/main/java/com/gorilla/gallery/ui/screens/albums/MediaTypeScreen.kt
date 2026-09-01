package com.gorilla.gallery.ui.screens.albums

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.MediaGrid
import com.gorilla.gallery.ui.components.MediaSection
import com.gorilla.gallery.ui.components.SelectionActionBar
import com.gorilla.gallery.ui.components.shareItems
import com.gorilla.gallery.ui.screens.albums.groupByGranularity
import com.gorilla.gallery.ui.theme.SpringSpecs

@Composable
fun MediaTypeScreen(
    app: AppViewModel,
    title: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewerOpen: Boolean = false,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
    predicate: (MediaItem) -> Boolean,
) {
    val allItems by app.container.mediaRepository.items.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by app.settings.collectAsStateWithLifecycle()
    val focusedItem by app.focusedItem.collectAsStateWithLifecycle()
    val items = remember(allItems, predicate) { allItems.filter(predicate) }
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All Media") }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Date Taken") }
    var sortAscending by remember { mutableStateOf(false) }

    val filteredItems = remember(items, selectedFilter, searchQuery, sortMode, sortAscending) {
        val filteredByFilter = when (selectedFilter) {
            "Photos" -> items.filter { !it.isVideo }
            "Videos" -> items.filter { it.isVideo }
            else -> items
        }
        val searched = if (searchQuery.isBlank()) {
            filteredByFilter
        } else {
            filteredByFilter.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
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
    
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var isSelecting by remember { mutableStateOf(false) }
    val selectionMode = isSelecting || selected.isNotEmpty()
    fun clear() { selected = emptySet(); isSelecting = false }
    val selectedItems = remember(selected, items) { items.filter { it.id in selected } }
    BackHandler(enabled = selectionMode) { clear() }
    
    var headerHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val topPadding = with(androidx.compose.ui.platform.LocalDensity.current) { headerHeightPx.toDp() }
    val gridBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    var gridColumns by remember { mutableStateOf(settings.gridColumns) }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.layerBackdrop(gridBackdrop)) {
            com.gorilla.gallery.ui.components.SortTransition(
                sortState = Pair(sortMode, sortAscending),
                contentKey = { it }
            ) { state ->
                val (currentSortMode, currentSortAscending) = state
                val currentItems = remember(items, selectedFilter, searchQuery, currentSortMode, currentSortAscending) {
                    val filteredByFilter = if (selectedFilter == "All") items else items.filter {
                        when (selectedFilter) {
                            "Photos" -> !it.isVideo
                            "Videos" -> it.isVideo
                            else -> true
                        }
                    }
                    val searched = if (searchQuery.isBlank()) {
                        filteredByFilter
                    } else {
                        filteredByFilter.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
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
                    sections = if (currentItems.isEmpty()) emptyList() else {
                        if (currentSortMode == "Date Taken" || currentSortMode == "Date Modified") {
                            groupByGranularity(currentItems, settings.dateGranularity)
                        } else {
                            listOf(MediaSection(key = "", label = "", items = currentItems))
                        }
                    },
                    columns = gridColumns,
                    selectionMode = selectionMode,
                    selectedIds = selected,
                    highQualityThumbnails = settings.highQualityThumbnails,
                    onColumnsChange = { gridColumns = it },
                    onClick = { item, bounds ->
                        if (selectionMode) {
                            selected = if (item.id in selected) selected - item.id else selected + item.id
                        } else {
                            onOpenViewer(items, item.uri, bounds)
                        }
                    },
                    onLongClick = { item -> selected = selected + item.id },
                    dragSelectionEnabled = true,
                    onDragSelectionChange = { selected = it },
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding() + topPadding,
                        bottom = contentPadding.calculateBottomPadding()
                    ),
                    headerContent = null,
                    maxColumns = 6,
                )
            }
        }

        com.gorilla.gallery.ui.components.AlbumHeader(
            title = title,
            eyebrow = "Media Type",
            countText = "${filteredItems.size} Items",
            onBack = onBack,
            onSelectToggle = {
                if (selectionMode) clear() else isSelecting = true
            },
            isSelectionMode = selectionMode,
            filters = listOf("All Media", "Photos", "Videos"),
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            sortMode = sortMode,
            onSortModeChange = { sortMode = it },
            sortAscending = sortAscending,
            onSortAscendingChange = { sortAscending = it },
            onSelectAll = { selected = filteredItems.map { it.id }.toSet(); isSelecting = true },
            onRenameAlbum = null,
            onShareAlbum = { com.gorilla.gallery.ui.components.shareItems(context, filteredItems) },
            onExportAlbum = { com.gorilla.gallery.ui.components.exportAsZip(context, filteredItems, title) },
            backdrop = gridBackdrop,
            modifier = Modifier.align(Alignment.TopCenter).onGloballyPositioned { headerHeightPx = it.size.height }
        )

        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically(SpringSpecs.OffsetSpring) { it } + fadeIn(SpringSpecs.Standard),
            exit = slideOutVertically(SpringSpecs.OffsetSpring) { it } + fadeOut(SpringSpecs.Standard),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 82.dp),
        ) {
            SelectionActionBar(
                count = selected.size,
                onFavorite = { selectedItems.forEach { app.toggleFavorite(it) }; clear() },
                onShare = { shareItems(context, selectedItems) },
                onSecure = { app.moveToSecure(selectedItems); clear() },
                onDelete = { app.moveToTrash(selectedItems); clear() },
                backdrop = gridBackdrop,
            )
        }
    }
}
