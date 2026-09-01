package com.gorilla.gallery.ui.screens.timeline

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.vectorResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.EmptyState
import com.gorilla.gallery.ui.components.MediaGrid
import com.gorilla.gallery.ui.components.SelectionActionBar
import com.gorilla.gallery.ui.components.shareItems
import com.gorilla.gallery.ui.theme.SpringSpecs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.pressScale

@Composable
fun TimelineScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    viewerOpen: Boolean,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
    vm: TimelineViewModel = viewModel(factory = TimelineViewModel.Factory),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val settings by app.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val galleryApp = context.applicationContext as com.gorilla.gallery.GalleryApp
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Sections only exist in the Content state; everything below treats "no content" uniformly.
    val sections = (uiState as? TimelineUiState.Content)?.sections ?: emptyList()
    val flat = remember(sections) { sections.flatMap { it.items } }
    val gridIndexByMediaId = remember(sections) {
        buildMap {
            var lazyIndex = 1 // top title item
            sections.forEach { section ->
                if (section.label.isNotEmpty()) lazyIndex += 1
                section.items.forEach { item ->
                    put(item.id, lazyIndex)
                    lazyIndex += 1
                }
            }
        }
    }
    var isSelecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val selectionMode = isSelecting || selected.isNotEmpty()
    fun clear() { 
        selected = emptySet() 
        isSelecting = false 
    }
    val selectedItems = remember(selected, flat) { flat.filter { it.id in selected } }
    BackHandler(enabled = selectionMode) { clear() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            app.importPhotos(uris)
        }
    )

    var showSortDialog by remember { mutableStateOf(false) }

    val sortAscending by vm.sortAscending.collectAsStateWithLifecycle()
    val sortMode by vm.sortMode.collectAsStateWithLifecycle()
    var gridState by remember { mutableStateOf(androidx.compose.foundation.lazy.grid.LazyGridState()) }

    val windowBounds = remember(density, configuration.screenWidthDp, configuration.screenHeightDp) {
        with(density) {
            Rect(
                left = 0f,
                top = 0f,
                right = configuration.screenWidthDp.dp.toPx(),
                bottom = configuration.screenHeightDp.dp.toPx(),
            )
        }
    }
    val thumbnailSizePx = remember(settings.gridColumns, density, configuration.screenWidthDp, settings.highQualityThumbnails) {
        with(density) {
            val screenWidthPx = configuration.screenWidthDp.dp.roundToPx()
            val spacingPx = (1.5.dp * 2).roundToPx()
            val sizePx = ((screenWidthPx / settings.gridColumns) - spacingPx).coerceAtLeast(1)
            if (settings.highQualityThumbnails) sizePx * 2 else sizePx
        }
    }

    // Tab-row visibility is position-driven (scroll past the top → tabs, return to top → bottom
    // nav). The Photos pill sets forceShowBottomNav, which short-circuits the position check so the
    // bottom nav appears instantly even mid-fling — no waiting for scroll momentum to settle. It
    val focusedItem by app.focusedItem.collectAsStateWithLifecycle()
    var lastSyncedViewerItemId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(viewerOpen, focusedItem, gridIndexByMediaId) {
        if (!viewerOpen) {
            lastSyncedViewerItemId = null
            return@LaunchedEffect
        }
        val item = focusedItem ?: return@LaunchedEffect
        val previousItemId = lastSyncedViewerItemId
        lastSyncedViewerItemId = item.id
        if (previousItemId == null) return@LaunchedEffect
        val index = gridIndexByMediaId[item.id] ?: return@LaunchedEffect
        
        // Use the new index to check visibility instead of relying on stale layoutInfo keys
        val firstVisible = gridState.firstVisibleItemIndex
        val visibleCount = gridState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        val lastVisible = firstVisible + visibleCount
        val isVisible = index in firstVisible..lastVisible
        
        if (!isVisible) {
            gridState.scrollToItem(index.coerceAtLeast(0))
        }
    }
    val resetToTop by app.resetToTop.collectAsStateWithLifecycle()
    LaunchedEffect(resetToTop) {
        if (resetToTop) {
            gridState.animateScrollToItem(0)
            app.setResetToTop(false)
        }
    }

    LaunchedEffect(flat, thumbnailSizePx) {
        withContext(Dispatchers.IO) {
            flat.forEachIndexed { index, item ->
                app.container.thumbnailRepository.load(
                    uri = item.uri,
                    mediaId = item.id,
                    sizePx = thumbnailSizePx,
                    cacheVersion = item.dateModifiedSec,
                    highQuality = settings.highQualityThumbnails,
                )
                if (index % 24 == 0) yield()
            }
        }
    }

    LaunchedEffect(flat, thumbnailSizePx) {
        var previousFirstVisible = -1
        snapshotFlow { gridState.firstVisibleItemIndex }
            .map { firstVisible ->
                val direction = when {
                    previousFirstVisible == -1 || firstVisible == previousFirstVisible -> 0
                    firstVisible > previousFirstVisible -> 1
                    else -> -1
                }
                previousFirstVisible = firstVisible
                firstVisible to direction
            }
            .distinctUntilChanged()
            .collect { (firstVisible, direction) ->
                if (flat.isEmpty()) return@collect
                val preloadIndices = when {
                    direction >= 0 -> (firstVisible + 6)..(firstVisible + 96)
                    else -> (firstVisible - 96)..(firstVisible - 6)
                }
                withContext(Dispatchers.IO) {
                    preloadIndices.forEach { index ->
                        val item = flat.getOrNull(index) ?: return@forEach
                        app.container.thumbnailRepository.load(
                            uri = item.uri,
                            mediaId = item.id,
                            sizePx = thumbnailSizePx,
                            cacheVersion = item.dateModifiedSec,
                            highQuality = settings.highQualityThumbnails,
                        )
                    }
                }
            }
    }

    val gridBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()

    Box(
        Modifier
            .fillMaxSize()
            .background(com.gorilla.gallery.ui.theme.DesignTokens.BgBase)
    ) {
        if (uiState is TimelineUiState.Loading) {
            Box(
                Modifier
                    .fillMaxSize()
            )
        } else if (uiState is TimelineUiState.Empty) {
            Column(Modifier.fillMaxSize()) {
                PhotosMasthead(
                    itemCount = 0,
                    selectionMode = selectionMode,
                    onSelectToggle = {
                        if (selectionMode) {
                            clear()
                        } else {
                            isSelecting = true
                        }
                    },
                    onSortClick = { showSortDialog = true },
                    onImportClick = {
                        importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                )
                EmptyState(
                    title = "No photos yet",
                    subtitle = "Photos and videos on your device will appear here.",
                    icon = Icons.Rounded.PhotoLibrary,
                )
            }
        } else {
            AnimatedVisibility(
                visible = uiState is TimelineUiState.Content,
                enter = fadeIn(animationSpec = com.gorilla.gallery.ui.theme.SpringSpecs.Standard)
            ) {
                var headerHeight by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
                val headerHeightDp = with(LocalDensity.current) { headerHeight.toDp() }
                
                Box(Modifier.layerBackdrop(gridBackdrop).fillMaxSize()) {
                    com.gorilla.gallery.ui.components.SortTransition(
                        sortState = Triple(sortMode, sortAscending, sections),
                        contentKey = { it.first to it.second }
                    ) { state ->
                        MediaGrid(
                            sections = state.third,
                            columns = settings.gridColumns,
                            selectionMode = selectionMode,
                            selectedIds = selected,
                            onColumnsChange = { vm.setColumns(it) },
                            onClick = { item, bounds ->
                                if (selectionMode) {
                                    selected = if (item.id in selected) selected - item.id else selected + item.id
                                } else {
                                    onOpenViewer(flat, item.uri, bounds)
                                }
                            },
                            onLongClick = { item -> selected = selected + item.id },
                            dragSelectionEnabled = true,
                            onDragSelectionChange = { selected = it },
                            contentPadding = PaddingValues(
                                top = headerHeightDp,
                                bottom = contentPadding.calculateBottomPadding() + 120.dp
                            ),
                            state = androidx.compose.foundation.lazy.grid.rememberLazyGridState().also { state ->
                                androidx.compose.runtime.SideEffect {
                                    gridState = state
                                }
                            },
                            headerContent = null,
                            viewerOpen = viewerOpen,
                            focusedItem = focusedItem,
                            highQualityThumbnails = settings.highQualityThumbnails,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(com.gorilla.gallery.ui.theme.DesignTokens.BgBase)
                            .onSizeChanged { headerHeight = it.height }
                    ) {
                        PhotosMasthead(
                            itemCount = flat.size,
                            selectionMode = selectionMode,
                            onSelectToggle = {
                                if (selectionMode) {
                                    clear()
                                } else {
                                    isSelecting = true
                                }
                            },
                            onSortClick = { showSortDialog = true },
                            onImportClick = {
                                importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            }
                        )
                    }
                }
            }
        }



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

        if (showSortDialog) {
            com.gorilla.gallery.ui.components.AnimatedGlassDialog(
                onDismissRequest = { showSortDialog = false }
            ) { scale ->
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                
                com.gorilla.gallery.ui.theme.LiquidGlassSurface(
                    depth = com.gorilla.gallery.ui.theme.GlassDepth.MID, shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    backdrop = gridBackdrop,
                    surfaceColor = DesignTokens.BgSurface.copy(alpha = 0.92f),
                    saturationOverride = 1.55f, tintAlphaOverride = 0.07f,
                    modifier = Modifier.widthIn(min = 300.dp, max = if (isLandscape) 560.dp else 300.dp).scale(scale).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = {}),
            ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            val accent = com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent
                            val haptic = com.gorilla.gallery.ui.theme.rememberHaptic()
                            androidx.compose.material3.Text(text = "Sort & Order Options", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = DesignTokens.TextPrimary, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp))
                            
                            @Composable
                            fun SortOption(label: String, selected: Boolean, onClick: () -> Unit) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                        .background(if (selected) accent.copy(alpha=0.15f) else Color.Transparent)
                                        .clickable { haptic(); onClick(); showSortDialog = false }
                                        .padding(horizontal = 10.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Text(
                                        text = label, 
                                        fontSize = 17.sp, 
                                        color = if (selected) accent else DesignTokens.TextPrimary, 
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            @Composable fun OrderOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                        .background(if (selected) accent.copy(alpha=0.15f) else Color.Transparent)
                                        .clickable { haptic(); onClick(); showSortDialog = false }
                                        .padding(horizontal = 10.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Text(text = label, fontSize = 17.sp, color = if (selected) accent else DesignTokens.TextPrimary, modifier = Modifier.weight(1f))
                                    androidx.compose.material3.Icon(imageVector = icon, contentDescription = label, tint = if (selected) accent else DesignTokens.TextPrimary, modifier = Modifier.size(20.dp))
                                }
                            }

                            if (isLandscape) {
                                Row(modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        androidx.compose.material3.Text("Sort By", color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                        SortOption("Date Taken", sortMode == "Date Taken") { vm.setSortMode("Date Taken"); vm.setSortAscending(false) }
                                        SortOption("Date Modified", sortMode == "Date Modified") { vm.setSortMode("Date Modified"); vm.setSortAscending(false) }
                                        SortOption("Name (A-Z)", sortMode == "Name (A-Z)") { vm.setSortMode("Name (A-Z)"); vm.setSortAscending(true) }
                                        SortOption("File Size", sortMode == "File Size") { vm.setSortMode("File Size"); vm.setSortAscending(false) }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        androidx.compose.material3.Text("Order", color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                        OrderOption("Ascending", Icons.Rounded.ArrowUpward, sortAscending) { vm.setSortAscending(true) }
                                        OrderOption("Descending", Icons.Rounded.ArrowDownward, !sortAscending) { vm.setSortAscending(false) }
                                        
                                        Spacer(modifier = Modifier.weight(1f))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            androidx.compose.material3.TextButton(onClick = { showSortDialog = false }, modifier = Modifier.pressScale(scale = 0.94f)) {
                                                androidx.compose.material3.Text(text = "Close", style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = accent)
                                            }
                                        }
                                    }
                                }
                            } else {
                                androidx.compose.material3.Text("Sort By", color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                SortOption("Date Taken", sortMode == "Date Taken") { vm.setSortMode("Date Taken"); vm.setSortAscending(false) }
                                SortOption("Date Modified", sortMode == "Date Modified") { vm.setSortMode("Date Modified"); vm.setSortAscending(false) }
                                SortOption("Name (A-Z)", sortMode == "Name (A-Z)") { vm.setSortMode("Name (A-Z)"); vm.setSortAscending(true) }
                                SortOption("File Size", sortMode == "File Size") { vm.setSortMode("File Size"); vm.setSortAscending(false) }

                                Box(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.Text("Order", color = com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                OrderOption("Ascending", Icons.Rounded.ArrowUpward, sortAscending) { vm.setSortAscending(true) }
                                OrderOption("Descending", Icons.Rounded.ArrowDownward, !sortAscending) { vm.setSortAscending(false) }
                                
                                Box(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    androidx.compose.material3.TextButton(onClick = { showSortDialog = false }, modifier = Modifier.pressScale(scale = 0.94f)) {
                                        androidx.compose.material3.Text(text = "Close", style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = accent)
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
fun PhotosMasthead(
    itemCount: Int,
    selectionMode: Boolean,
    onSelectToggle: () -> Unit,
    onSortClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val accent = com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent
    val appColors = com.gorilla.gallery.ui.theme.LocalAppColors.current
    
    val importIcon = remember {
        ImageVector.Builder(
            name = "Import",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(21f, 15f); lineTo(21f, 19f)
                arcTo(2f, 2f, 0f, false, true, 19f, 21f); lineTo(5f, 21f)
                arcTo(2f, 2f, 0f, false, true, 3f, 19f); lineTo(3f, 15f)
                moveTo(17f, 8f); lineTo(12f, 3f); lineTo(7f, 8f)
                moveTo(12f, 3f); lineTo(12f, 15f)
            }
        }.build()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = "Photos",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        
        // Right Tools Capsule
        Row(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .background(appColors.bgSurface.copy(alpha = 0.9f), androidx.compose.foundation.shape.RoundedCornerShape(25.dp))
                .border(1.dp, appColors.borderGlass, androidx.compose.foundation.shape.RoundedCornerShape(25.dp))
                .padding(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Sort Icon
            Box(
                modifier = Modifier
                    .pressScale(0.94f)
                    .size(32.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(appColors.textPrimary.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onSortClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = com.gorilla.gallery.R.drawable.ic_sort_custom),
                    contentDescription = "Sort Options",
                    tint = appColors.textPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }
            
            // Import Button
            Row(
                modifier = Modifier
                    .pressScale(0.94f)
                    .height(32.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.32f), androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onImportClick
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = importIcon,
                    contentDescription = "Import",
                    tint = accent,
                    modifier = Modifier.size(13.dp)
                )
                androidx.compose.material3.Text(
                    text = "Import",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.01.sp
                )
            }
        }
    }
}
