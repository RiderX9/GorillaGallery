package com.gorilla.gallery.ui.nav

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.gorilla.gallery.ui.theme.DesignTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.screens.albums.AlbumDetailScreen
import com.gorilla.gallery.ui.screens.albums.AlbumsScreen
import com.gorilla.gallery.ui.screens.albums.MediaTypeScreen
import com.gorilla.gallery.ui.screens.albums.groupByGranularity
import com.gorilla.gallery.ui.screens.favorites.FavoritesScreen
import com.gorilla.gallery.ui.screens.search.SearchScreen
import com.gorilla.gallery.ui.screens.settings.SettingsScreen
import com.gorilla.gallery.ui.screens.timeline.TimelineScreen
import com.gorilla.gallery.ui.screens.editor.EditorScreen
import com.gorilla.gallery.ui.screens.videoeditor.VideoEditorScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.onGloballyPositioned
import com.gorilla.gallery.ui.components.MediaGrid
import com.gorilla.gallery.ui.components.MediaSection
import com.gorilla.gallery.ui.components.SelectionActionBar
import com.gorilla.gallery.ui.components.shareItems
import com.gorilla.gallery.ui.theme.SpringSpecs
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.dp

@Composable
fun GalleryNavHost(
    navController: NavHostController,
    app: AppViewModel,
    viewerOpen: Boolean,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
    editorFallbackItem: MediaItem?,
    onEditorClosed: () -> Unit,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Timeline.route,
        enterTransition = {
            tabEnterTransition(
                initialRoute = initialState.destination.route,
                targetRoute = targetState.destination.route,
            )
        },
        exitTransition = {
            // Hard cut (no fade) when opening the editor. Otherwise the outgoing grid screen plays
            // its ~0.5s fadeOut ACROSS the entering editor — that lingering grid-over-photo blend is
            // the "editor glitches the photo for half a second". With the editor route's
            // EnterTransition.None this makes opening the editor an instant, flash-free swap.
            tabExitTransition(
                initialRoute = initialState.destination.route,
                targetRoute = targetState.destination.route,
            )
        },
        popEnterTransition = {
            tabEnterTransition(
                initialRoute = initialState.destination.route,
                targetRoute = targetState.destination.route,
            )
        },
        popExitTransition = {
            tabExitTransition(
                initialRoute = initialState.destination.route,
                targetRoute = targetState.destination.route,
            )
        },
    ) {
        composable(Destination.Timeline.route) {
            TimelineScreen(app, contentPadding, viewerOpen, onOpenViewer)
        }
        composable(Destination.Albums.route) {
            AlbumsScreen(
                app = app,
                contentPadding = contentPadding,
                onOpenAlbum = { bucketId -> navController.navigate(Routes.albumDetail(bucketId)) },
                onOpenSecure = { navController.navigate(Routes.SecureFolder) },
                onOpenPhotos = {
                    app.collapseAlbums()
                    navController.navigate(Routes.Photos)
                },
                onOpenVideos = {
                    app.collapseAlbums()
                    navController.navigate(Routes.Videos)
                },
                onOpenSelfies = {
                    app.collapseAlbums()
                    navController.navigate(Routes.Selfies)
                },
                onOpenScreenshots = {
                    app.collapseAlbums()
                    navController.navigate(Routes.Screenshots)
                },
                onOpenFavorites = {
                    app.collapseAlbums()
                    navController.navigate(Routes.Favorites) {
                        launchSingleTop = true
                    }
                },
                onOpenEdited = {
                    app.collapseAlbums()
                    navController.navigate(Routes.Edited)
                },
                onOpenTrash = {
                    app.collapseAlbums()
                    navController.navigate(Routes.Trash)
                },
                onOpenViewer = onOpenViewer,
            )
        }
        composable(Routes.Trash) {
            val trashItems by app.container.trashRepository.items.collectAsStateWithLifecycle(initialValue = emptyList<com.gorilla.gallery.data.model.MediaItem>())
            com.gorilla.gallery.ui.screens.albums.TrashScreen(
                app = app,
                viewerOpen = viewerOpen,
                items = trashItems,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
            )
        }
        composable(Routes.SecureFolder) {
            com.gorilla.gallery.ui.screens.securefolder.SecureFolderScreen(
                app = app,
                viewerOpen = viewerOpen,
                onClose = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
            )
        }
        composable(Routes.Favorites) {
            FavoritesScreen(
                app = app,
                viewerOpen = viewerOpen,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer
            )
        }
        composable(
            route = "${Destination.Search.route}?autoFocus={autoFocus}",
            arguments = listOf(navArgument("autoFocus") {
                type = NavType.BoolType
                defaultValue = false
            }),
        ) { backStackEntry ->
            val autoFocus = backStackEntry.arguments?.getBoolean("autoFocus") ?: false
            SearchScreen(
                app = app,
                viewerOpen = viewerOpen,
                contentPadding = contentPadding,
                onOpenViewer = onOpenViewer,
                autoFocus = autoFocus,
                onOpenVideos = { navController.navigate(Routes.Videos) },
                onOpenFavorites = { navController.navigate(Routes.Favorites) { launchSingleTop = true } },
                onOpenRaw = { navController.navigate(Routes.Raw) },
                onOpenPanoramas = { navController.navigate(Routes.Panoramas) },
                onOpenPersonDetail = { clusterId -> navController.navigate("${Routes.PersonDetail}/$clusterId") },
                onOpenSeeAllPeople = { navController.navigate("${Routes.PersonDetail}/-2") },
            )
        }
        composable(Destination.Settings.route) {
            SettingsScreen(app, contentPadding)
        }
        composable(Routes.Photos) {
            MediaTypeScreen(
                app = app,
                viewerOpen = viewerOpen,
                title = "Photos",
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
                predicate = { !it.isVideo },
            )
        }
        composable(Routes.Selfies) {
            val selfiePaths by app.container.faceIndexRepository.selfiePaths.collectAsStateWithLifecycle()
            MediaTypeScreen(
                app = app,
                viewerOpen = viewerOpen,
                title = "Selfies",
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
                predicate = { !it.isVideo && it.uri.toString() in selfiePaths },
            )
        }
        composable(Routes.Screenshots) {
            MediaTypeScreen(
                app = app,
                viewerOpen = viewerOpen,
                title = "Screenshots",
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
                predicate = { it.isScreenshot },
            )
        }
        composable(Routes.Edited) {
            MediaTypeScreen(
                app = app,
                viewerOpen = viewerOpen,
                title = "Edited",
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
                predicate = { it.isEdited },
            )
        }
        composable(
            route = "${Routes.AlbumDetail}/{bucketId}",
            arguments = listOf(navArgument("bucketId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: -1L
            AlbumDetailScreen(
                app = app,
                viewerOpen = viewerOpen,
                bucketId = bucketId,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
            )
        }
        composable(Routes.Videos) {
            MediaTypeScreen(
                app = app,
                viewerOpen = viewerOpen,
                title = "Videos",
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
                predicate = { it.isVideo },
            )
        }
        composable(Routes.Raw) {
            MediaTypeScreen(
                app = app,
                viewerOpen = viewerOpen,
                title = "RAW Shots",
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
                predicate = { !it.isVideo && it.mimeType.contains("raw", ignoreCase = true) },
            )
        }
        composable(Routes.Panoramas) {
            MediaTypeScreen(
                app = app,
                viewerOpen = viewerOpen,
                title = "Panoramas",
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenViewer = onOpenViewer,
                predicate = { 
                    !it.isVideo && it.width > 0 && it.height > 0 && 
                    (it.width.toFloat() / it.height.toFloat() > 2.5f || it.height.toFloat() / it.width.toFloat() > 2.5f)
                },
            )
        }
        composable(
            route = "${Routes.PersonDetail}/{clusterId}",
            arguments = listOf(navArgument("clusterId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val clusterId = backStackEntry.arguments?.getInt("clusterId") ?: -1
            val isAll = clusterId == -2
            var resolvedName by remember { mutableStateOf(if (isAll) "People & Pets" else "Person") }
            var resolvedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
            var refreshTrigger by remember { mutableIntStateOf(0) }

            LaunchedEffect(clusterId, refreshTrigger) {
                resolvedPaths = withContext(Dispatchers.IO) {
                    if (isAll) {
                        val cats = app.container.peopleRepository.getCategories()
                        cats.flatMapTo(mutableSetOf()) { it.imagePaths }
                    } else {
                        val category = app.container.peopleRepository.getCategories()
                            .firstOrNull { it.clusterId == clusterId }
                        if (category != null) {
                            resolvedName = category.label
                            category.imagePaths
                        } else {
                            emptySet()
                        }
                    }
                }
            }

            val allItems by app.container.mediaRepository.items.collectAsStateWithLifecycle(emptyList())
            val filteredItems = remember(allItems, resolvedPaths) {
                if (resolvedPaths.isEmpty()) emptyList()
                else allItems.filter { it.uri.toString() in resolvedPaths && !it.isScreenshot }
            }
            val settings by app.settings.collectAsStateWithLifecycle()
            
            var selectedFilter by remember { mutableStateOf("All Media") }
            var searchQuery by remember { mutableStateOf("") }
            var sortMode by remember { mutableStateOf("Date Taken") }
            var sortAscending by remember { mutableStateOf(false) }

            val baseFilteredItems = remember(filteredItems, selectedFilter, searchQuery) {
                val filteredByFilter = when (selectedFilter) {
                    "Photos" -> filteredItems.filter { !it.isVideo }
                    "Videos" -> filteredItems.filter { it.isVideo }
                    else -> filteredItems
                }
                if (searchQuery.isBlank()) {
                    filteredByFilter
                } else {
                    filteredByFilter.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
                }
            }
            
            // finalFilteredItems is still needed for AlbumHeader count and selectAll
            val finalFilteredItems = remember(baseFilteredItems, sortMode, sortAscending) {
                val sorted = when (sortMode) {
                    "Date Taken" -> baseFilteredItems.sortedBy { it.dateTakenMs }
                    "Date Modified" -> baseFilteredItems.sortedBy { it.dateModifiedSec }
                    "Name (A-Z)" -> baseFilteredItems.sortedBy { it.displayName }
                    "File Size" -> baseFilteredItems.sortedBy { it.sizeBytes }
                    else -> baseFilteredItems.sortedBy { it.dateTakenMs }
                }
                if (sortAscending) sorted else sorted.reversed()
            }

            var selected by remember { mutableStateOf(setOf<Long>()) }
            var isSelecting by remember { mutableStateOf(false) }
            val selectionMode = isSelecting || selected.isNotEmpty()
            fun clear() { selected = emptySet(); isSelecting = false }
            val selectedItems = remember(selected, finalFilteredItems) { finalFilteredItems.filter { it.id in selected } }
            
            val coroutineScope = rememberCoroutineScope()
            var gridColumns by remember { mutableStateOf(settings.gridColumns) }
            
            androidx.compose.runtime.DisposableEffect(Unit) {
                onDispose { selected = emptySet() }
            }
            androidx.activity.compose.BackHandler(enabled = selectionMode) { clear() }
            
            var headerHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
            val topPadding = with(androidx.compose.ui.platform.LocalDensity.current) { headerHeightPx.toDp() }
            
            val gridBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
            val context = androidx.compose.ui.platform.LocalContext.current

            val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
            ) { uris ->
                if (uris.isNotEmpty() && clusterId >= 0) {
                    val paths = uris.map { it.toString() }
                    app.container.peopleRepository.addPhotosToPerson(clusterId, paths)
                    refreshTrigger++
                }
            }

            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(Modifier.layerBackdrop(gridBackdrop)) {
                    com.gorilla.gallery.ui.components.SortTransition(
                        sortState = Pair(sortMode, sortAscending),
                        contentKey = { it }
                    ) { state ->
                        val (currentSortMode, currentSortAscending) = state
                        val currentItems = remember(baseFilteredItems, currentSortMode, currentSortAscending) {
                            val sorted = when (currentSortMode) {
                                "Date Taken" -> baseFilteredItems.sortedBy { it.dateTakenMs }
                                "Date Modified" -> baseFilteredItems.sortedBy { it.dateModifiedSec }
                                "Name (A-Z)" -> baseFilteredItems.sortedBy { it.displayName }
                                "File Size" -> baseFilteredItems.sortedBy { it.sizeBytes }
                                else -> baseFilteredItems.sortedBy { it.dateTakenMs }
                            }
                            if (currentSortAscending) sorted else sorted.reversed()
                        }
                        
                        MediaGrid(
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
                                    onOpenViewer(currentItems, item.uri, bounds)
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
                        )
                    }
                }

                com.gorilla.gallery.ui.components.AlbumHeader(
                    title = resolvedName,
                    eyebrow = if (isAll) "Collection" else "Person",
                    countText = "${finalFilteredItems.size} Items",
                    onBack = { navController.popBackStack() },
                    onSelectToggle = {
                        if (selectionMode) clear() else isSelecting = true
                    },
                    customActionText = if (isAll || selectionMode) null else "Add",
                    onCustomAction = if (isAll || selectionMode) null else {
                        { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    },
                    isSelectionMode = selectionMode,
                    filters = emptyList(),
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    sortMode = sortMode,
                    onSortModeChange = { sortMode = it },
                    sortAscending = sortAscending,
                    onSortAscendingChange = { sortAscending = it },
                    onSelectAll = { selected = finalFilteredItems.map { it.id }.toSet(); isSelecting = true },
                    onRenameAlbum = if (isAll) null else { newName ->
                        coroutineScope.launch {
                            app.container.peopleRepository.renamePerson(clusterId, newName)
                            resolvedName = newName
                            refreshTrigger++
                        }
                    },
                    onShareAlbum = { com.gorilla.gallery.ui.components.shareItems(context, finalFilteredItems) },
                    onExportAlbum = { com.gorilla.gallery.ui.components.exportAsZip(context, finalFilteredItems, resolvedName) },
                    backdrop = gridBackdrop,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .onGloballyPositioned { headerHeightPx = it.size.height }
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
        composable(
            route = "editor/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
            // Enter instantly opaque instead of crossfading. The editor is pushed on top, so a fade
            // would blend it with the grid screen below for the whole ~0.5s spring — that blend is
            // the "editor glitch". Appearing at full opacity covers the grid immediately; the
            // outgoing grid fades out behind the opaque editor, unseen. Pop still fades (global).
            enterTransition = { EnterTransition.None },
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: ""
            val decodedUri = remember(uriString) { android.net.Uri.parse(android.net.Uri.decode(uriString)) }
            val items by app.container.mediaRepository.items.collectAsStateWithLifecycle(initialValue = emptyList())
            val mediaItem = remember(items, decodedUri) {
                items.firstOrNull { it.uri == decodedUri }
            } ?: editorFallbackItem?.takeIf { it.uri == decodedUri }
            // Opaque base fills the route from its first frame so the grid behind is covered even
            // before the media list emits and mediaItem resolves — otherwise the editor route is
            // empty for a frame and the grid flashes through during the enter animation.
            Box(Modifier.fillMaxSize().background(DesignTokens.BgBase)) {
                mediaItem?.let { item ->
                    EditorScreen(
                        app = app,
                        item = item,
                        onClose = {
                            onEditorClosed()
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
        composable(
            route = "videoEditor/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
            enterTransition = { EnterTransition.None },
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: ""
            val decodedUri = remember(uriString) { android.net.Uri.parse(android.net.Uri.decode(uriString)) }
            val items by app.container.mediaRepository.items.collectAsStateWithLifecycle(initialValue = emptyList())
            val mediaItem = remember(items, decodedUri) {
                items.firstOrNull { it.uri == decodedUri }
            } ?: editorFallbackItem?.takeIf { it.uri == decodedUri }
            Box(Modifier.fillMaxSize().background(DesignTokens.BgBase)) {
                mediaItem?.takeIf { it.isVideo }?.let { item ->
                    VideoEditorScreen(
                        app = app,
                        item = item,
                        onClose = {
                            onEditorClosed()
                            navController.popBackStack()
                        },
            )
                }
            }
        }
    }
}

private fun tabEnterTransition(initialRoute: String?, targetRoute: String?): EnterTransition {
    val targetIsEditor = isEditorRoute(targetRoute)
    if (targetIsEditor) return EnterTransition.None

    val initialDepth = routeDepth(initialRoute)
    val targetDepth = routeDepth(targetRoute)

    if (initialDepth < targetDepth) {
        return slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(320))
    } else if (initialDepth > targetDepth) {
        return slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(animationSpec = tween(320))
    }

    val initialIndex = bottomTabIndex(initialRoute)
    val targetIndex = bottomTabIndex(targetRoute)

    return if (initialIndex != null && targetIndex != null && initialIndex != targetIndex) {
        val forward = targetIndex > initialIndex
        slideInHorizontally(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) { width ->
            if (forward) width / 5 else -width / 5
        } + fadeIn(animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
    } else {
        fadeIn(animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
    }
}

private fun tabExitTransition(initialRoute: String?, targetRoute: String?): ExitTransition {
    val targetIsEditor = isEditorRoute(targetRoute)
    if (targetIsEditor) return ExitTransition.None

    val initialDepth = routeDepth(initialRoute)
    val targetDepth = routeDepth(targetRoute)

    if (initialDepth < targetDepth) {
        return slideOutHorizontally(animationSpec = tween(260)) { -it / 3 } + fadeOut(animationSpec = tween(260))
    } else if (initialDepth > targetDepth) {
        return slideOutHorizontally(animationSpec = tween(260)) { it } + fadeOut(animationSpec = tween(260))
    }

    val initialIndex = bottomTabIndex(initialRoute)
    val targetIndex = bottomTabIndex(targetRoute)

    return if (initialIndex != null && targetIndex != null && initialIndex != targetIndex) {
        val forward = targetIndex > initialIndex
        slideOutHorizontally(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) { width ->
            if (forward) -width / 5 else width / 5
        } + fadeOut(animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing))
    } else {
        fadeOut(animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
    }
}

private fun bottomTabIndex(route: String?): Int? {
    val orderedTabs = listOf(Destination.Timeline, Destination.Albums, Destination.Search, Destination.Settings)
    return orderedTabs.indexOfFirst { destination -> route?.startsWith(destination.route) == true }
        .takeIf { it >= 0 }
}

private fun isEditorRoute(route: String?): Boolean =
    route?.startsWith("editor") == true || route?.startsWith("videoEditor") == true

private fun routeDepth(route: String?): Int {
    if (route == null) return 0
    val topLevel = Destination.entries.any { route.startsWith(it.route) }
    if (topLevel) return 0
    if (isEditorRoute(route)) return 0
    return 1
}
