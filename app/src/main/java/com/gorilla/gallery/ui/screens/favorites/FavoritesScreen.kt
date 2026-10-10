package com.gorilla.gallery.ui.screens.favorites

import androidx.compose.runtime.setValue
import androidx.compose.foundation.background

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
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.EmptyState
import com.gorilla.gallery.ui.components.MediaGrid
import com.gorilla.gallery.ui.components.MediaSection
import com.gorilla.gallery.ui.components.SelectionActionBar
import com.gorilla.gallery.ui.components.shareItems
import com.gorilla.gallery.ui.theme.SpringSpecs

@Composable
fun FavoritesScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewerOpen: Boolean = false,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
    vm: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory),
) {
    val allFavorites by vm.favorites.collectAsStateWithLifecycle(emptyList())
    val settings by app.settings.collectAsStateWithLifecycle()
    val focusedItem by app.focusedItem.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All Media") }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Date Taken") }
    var sortAscending by remember { mutableStateOf(false) }
    
    val favorites = remember(allFavorites, selectedFilter, searchQuery, sortMode, sortAscending) {
        val filteredByFilter = when (selectedFilter) {
            "Photos" -> allFavorites.filter { !it.isVideo }
            "Videos" -> allFavorites.filter { it.isVideo }
            else -> allFavorites
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
    val selectedItems = remember(selected, favorites) { favorites.filter { it.id in selected } }
    BackHandler(enabled = selectionMode) { clear() }
    
    var headerHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val topPadding = with(androidx.compose.ui.platform.LocalDensity.current) { headerHeightPx.toDp() }
    val gridBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.layerBackdrop(gridBackdrop).background(com.gorilla.gallery.ui.theme.DesignTokens.BgBase)) {
            if (allFavorites.isEmpty()) {
                Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        title = "No favorites yet",
                        subtitle = "Tap the heart on any photo to add it here.",
                        icon = Icons.Rounded.FavoriteBorder,
                    )
                }
            } else {
                com.gorilla.gallery.ui.components.SortTransition(
                    sortState = Pair(sortMode, sortAscending),
                    contentKey = { it }
                ) { state ->
                    val (currentSortMode, currentSortAscending) = state
                    val currentItems = remember(allFavorites, selectedFilter, searchQuery, currentSortMode, currentSortAscending) {
                        val filteredByFilter = when (selectedFilter) {
                            "Photos" -> allFavorites.filter { !it.isVideo }
                            "Videos" -> allFavorites.filter { it.isVideo }
                            else -> allFavorites
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
                        sections = if (currentItems.isEmpty()) emptyList() else if (currentSortMode == "Date Taken" || currentSortMode == "Date Modified") com.gorilla.gallery.ui.screens.timeline.groupByDate(currentItems, settings.dateGranularity, currentSortAscending) else {
                            val headerLabel = if (settings.dateGranularity == com.gorilla.gallery.data.settings.DateGranularity.ALL) "" else when (currentSortMode) {
                                "Name (A-Z)" -> if (currentSortAscending) "Name (A-Z)" else "Name (Z-A)"
                                "File Size" -> if (currentSortAscending) "File Size ↑" else "File Size ↓"
                                else -> currentSortMode
                            }
                            listOf(MediaSection(currentSortMode, headerLabel, currentItems))
                        },
                        columns = settings.gridColumns,
                        selectionMode = selectionMode,
                        selectedIds = selected,
                        highQualityThumbnails = settings.highQualityThumbnails,
                        onColumnsChange = { app.setGridColumns(it) },
                        maxColumns = 6,
                        onClick = { item, bounds ->
                            if (selectionMode) {
                                selected = if (item.id in selected) selected - item.id else selected + item.id
                            } else {
                                onOpenViewer(currentItems, item.uri, bounds)
                            }
                        },
                        onLongClick = { item -> selected = selected + item.id },
                        dragSelectionEnabled = true,
                        onDragSelectionChange = { selected = it },
                        contentPadding = PaddingValues(
                            top = topPadding,
                            bottom = contentPadding.calculateBottomPadding()
                        ),
                        headerContent = null
                    )
                }
            }
        }
        
        com.gorilla.gallery.ui.components.AlbumHeader(
            title = "Favorites",
            eyebrow = "Local Album",
            countText = "${favorites.size} Items",
            onBack = onBack,
            onSelectToggle = if (allFavorites.isEmpty()) null else { { if (selectionMode) clear() else isSelecting = true } },
            isSelectionMode = selectionMode,
            filters = if (allFavorites.isEmpty()) emptyList() else listOf("All Media", "Photos", "Videos"),
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            sortMode = sortMode,
            onSortModeChange = { sortMode = it },
            sortAscending = sortAscending,
            onSortAscendingChange = { sortAscending = it },
            onSelectAll = if (allFavorites.isEmpty()) null else { { selected = favorites.map { it.id }.toSet(); isSelecting = true } },
            onRenameAlbum = { android.widget.Toast.makeText(context, "Rename album not supported yet", android.widget.Toast.LENGTH_SHORT).show() },
            onShareAlbum = { com.gorilla.gallery.ui.components.shareItems(context, favorites) },
            onExportAlbum = { com.gorilla.gallery.ui.components.exportAsZip(context, favorites, "Favorites") },
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
                allSelected = favorites.isNotEmpty() && selected.size == favorites.size,
                onSelectAll = {
                    if (selected.size == favorites.size) {
                        clear()
                    } else {
                        selected = favorites.map { it.id }.toSet()
                    }
                },
                onFavorite = { selectedItems.forEach { app.toggleFavorite(it) }; clear() },
                onShare = { shareItems(context, selectedItems) },
                onSecure = { app.moveToSecure(selectedItems); clear() },
                onDelete = { app.moveToTrash(selectedItems); clear() },
                backdrop = gridBackdrop,
            )
        }
    }
}
