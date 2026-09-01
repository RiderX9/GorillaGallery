package com.gorilla.gallery.ui.screens.favorites

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
    var gridColumns by remember { mutableStateOf(settings.gridColumns) }
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All Media") }
    var searchQuery by remember { mutableStateOf("") }
    val favorites = remember(allFavorites, selectedFilter, searchQuery) {
        val filteredByFilter = when (selectedFilter) {
            "Photos" -> allFavorites.filter { !it.isVideo }
            "Videos" -> allFavorites.filter { it.isVideo }
            else -> allFavorites
        }
        if (searchQuery.isBlank()) {
            filteredByFilter
        } else {
            filteredByFilter.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
        }
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
        Box(Modifier.layerBackdrop(gridBackdrop)) {
            if (allFavorites.isEmpty()) {
                Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        title = "No favorites yet",
                        subtitle = "Tap the heart on any photo to add it here.",
                        icon = Icons.Rounded.FavoriteBorder,
                    )
                }
            } else {
                MediaGrid(
                    viewerOpen = viewerOpen,
                    focusedItem = focusedItem,
                    sections = if (favorites.isEmpty()) emptyList() else listOf(MediaSection("fav", "", favorites)),
                    columns = gridColumns,
                    selectionMode = selectionMode,
                    selectedIds = selected,
                    highQualityThumbnails = settings.highQualityThumbnails,
                    onColumnsChange = { gridColumns = it },
                    maxColumns = 6,
                    onClick = { item, bounds ->
                        if (selectionMode) {
                            selected = if (item.id in selected) selected - item.id else selected + item.id
                        } else {
                            onOpenViewer(favorites, item.uri, bounds)
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
                onFavorite = { selectedItems.forEach { app.toggleFavorite(it) }; clear() },
                onShare = { shareItems(context, selectedItems) },
                onSecure = { app.moveToSecure(selectedItems); clear() },
                onDelete = { app.moveToTrash(selectedItems); clear() },
                backdrop = gridBackdrop,
            )
        }
    }
}
