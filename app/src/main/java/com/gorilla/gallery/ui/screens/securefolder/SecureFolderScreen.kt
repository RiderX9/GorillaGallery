package com.gorilla.gallery.ui.screens.securefolder

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.EmptyState
import com.gorilla.gallery.ui.components.MediaGrid
import com.gorilla.gallery.ui.components.MediaSection
import com.gorilla.gallery.ui.theme.pressScale
import androidx.compose.ui.layout.onGloballyPositioned
import com.gorilla.gallery.ui.components.AlbumHeader
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size

/** Full-screen Secure Folder overlay: a gate until unlocked, then a private grid. */
@Composable
fun SecureFolderScreen(
    app: AppViewModel,
    onClose: () -> Unit,
    viewerOpen: Boolean = false,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
    vm: SecureFolderViewModel = viewModel(factory = SecureFolderViewModel.Factory),
) {
    val unlocked by app.secureUnlocked.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()

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

    androidx.activity.compose.BackHandler(onBack = onClose)
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            app.lockSecure()
        }
    }
    
    Box(Modifier.fillMaxSize().background(com.gorilla.gallery.ui.theme.DesignTokens.BgBase)) {
        if (!unlocked) {
            Box(Modifier.fillMaxSize()) {
                SecureGateScreen(app)
            }
        } else {
            var headerHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
            val topPadding = with(androidx.compose.ui.platform.LocalDensity.current) { headerHeightPx.toDp() }
            val gridBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
            
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize()) {
                    BackHeader(onClose, count = 0, onLock = { app.lockSecure() }, sortMode = sortMode, onSortModeChange = { sortMode = it }, sortAscending = sortAscending, onSortAscendingChange = { sortAscending = it }, selectedFilter = selectedFilter, onFilterSelected = { selectedFilter = it }, searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it }, backdrop = gridBackdrop, modifier = Modifier.align(Alignment.TopCenter).onGloballyPositioned { headerHeightPx = it.size.height })
                    EmptyState(
                        title = "Secure Folder is empty",
                        subtitle = "Move items here from their menu to hide them.",
                        icon = Icons.Rounded.Lock,
                    )
                }
            } else {
                Box(Modifier.layerBackdrop(gridBackdrop)) {
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
                            sections = if (currentItems.isEmpty()) emptyList() else listOf(MediaSection("secure", "", currentItems)),
                            columns = gridColumns,
                            selectionMode = false,
                            selectedIds = emptySet(),
                            highQualityThumbnails = settings.highQualityThumbnails,
                            onColumnsChange = { gridColumns = it },
                            maxColumns = 6,
                            onClick = { item, bounds ->
                                onOpenViewer(currentItems, item.uri, bounds)
                            },
                            onLongClick = {},
                            contentPadding = PaddingValues(top = topPadding, bottom = 24.dp),
                            headerContent = null
                        )
                    }
                }
                
                BackHeader(onClose, count = filteredItems.size, onLock = { app.lockSecure() }, sortMode = sortMode, onSortModeChange = { sortMode = it }, sortAscending = sortAscending, onSortAscendingChange = { sortAscending = it }, selectedFilter = selectedFilter, onFilterSelected = { selectedFilter = it }, searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it }, backdrop = gridBackdrop, modifier = Modifier.align(Alignment.TopCenter).onGloballyPositioned { headerHeightPx = it.size.height })
            }
        }
    }
}

@Composable
private fun BackHeader(
    onClose: () -> Unit,
    count: Int,
    onLock: () -> Unit,
    sortMode: String,
    onSortModeChange: (String) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    modifier: Modifier = Modifier
) {
    com.gorilla.gallery.ui.components.AlbumHeader(
        title = "Secure Folder",
        eyebrow = "",
        countText = "$count items",
        onBack = onClose,
        headerActions = {
            val accentColor = com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent
            Row(
                modifier = Modifier
                    .pressScale(0.94f)
                    .height(36.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.32f), androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                    .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onLock() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Lock",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                androidx.compose.material3.Text(
                    text = "Lock",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 0.01.sp
                )
            }
        },
        filters = if (count == 0 && selectedFilter == "All Media" && searchQuery.isBlank()) emptyList() else listOf("All Media", "Photos", "Videos"),
        selectedFilter = selectedFilter,
        onFilterSelected = onFilterSelected,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        sortMode = sortMode,
        onSortModeChange = onSortModeChange,
        sortAscending = sortAscending,
        onSortAscendingChange = onSortAscendingChange,
        showMoreMenu = false,
        backdrop = backdrop,
        modifier = modifier
    )
}
