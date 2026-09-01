package com.gorilla.gallery.ui

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.nav.Destination
import com.gorilla.gallery.ui.nav.GalleryNavHost
import com.gorilla.gallery.ui.nav.GlassNavigationBar
import com.gorilla.gallery.ui.screens.permission.PermissionGate
import com.gorilla.gallery.ui.screens.viewer.ViewerScreen
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.GalleryBackgroundHost
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.gallery.ui.theme.rememberHaptic
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Rect
import com.gorilla.gallery.ui.components.GlassIconPill

private data class ViewerRequest(
    val openId: Long,
    val items: List<MediaItem>,
    val clickedUri: android.net.Uri,
    val currentUri: android.net.Uri = clickedUri,
    val sourceBounds: Rect? = null,
    val onCloseReturn: (() -> Unit)? = null,
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GalleryRoot(
    app: AppViewModel,
    externalItem: MediaItem? = null,
    onExternalClose: (() -> Unit)? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            buildList {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.POST_NOTIFICATIONS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                }
            }
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)
    // Treat any granted media permission (incl. partial "selected photos") as proceed.
    val granted = permissionState.permissions.any { it.status.isGranted }

    LaunchedEffect(granted) { app.onPermissionResult(granted) }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val activeRoute = backStack?.destination?.route
    val isEditorOpen = activeRoute?.startsWith("editor") == true || activeRoute?.startsWith("videoEditor") == true

    val scope = rememberCoroutineScope()
    var viewerRequest by remember { mutableStateOf<ViewerRequest?>(null) }
    var activeViewerRequest by remember { mutableStateOf<ViewerRequest?>(null) }
    var editorFallbackItem by remember { mutableStateOf<MediaItem?>(null) }
    var viewerReturnAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var nextViewerOpenId by remember { mutableStateOf(0L) }
    var viewerSwipeClosing by remember { mutableStateOf(false) }
    val liveMediaItems by app.container.mediaRepository.items.collectAsStateWithLifecycle(initialValue = emptyList())
    LaunchedEffect(viewerRequest) {
        if (viewerRequest != null) activeViewerRequest = viewerRequest
    }
    LaunchedEffect(liveMediaItems) {
        val freshByUri = liveMediaItems.associateBy { it.uri }
        fun ViewerRequest.withFreshItems(): ViewerRequest {
            val updatedItems = items.map { item -> freshByUri[item.uri] ?: item }
            return if (updatedItems == items) this else copy(items = updatedItems)
        }
        viewerRequest = viewerRequest?.withFreshItems()
        activeViewerRequest = activeViewerRequest?.withFreshItems()
    }
    LaunchedEffect(viewerRequest?.items, viewerRequest?.currentUri) {
        val req = viewerRequest ?: return@LaunchedEffect
        val currentItem = req.items.firstOrNull { it.uri == req.currentUri } ?: return@LaunchedEffect
        app.onPhotoFocused(currentItem)
    }
    // Opaque backdrop mounted only WHILE the viewer is open, so the enter animation doesn't reveal
    // the grid through the fading-in viewer. It's torn down the instant the viewer closes so the
    // grid is shown right away — holding it past the close was what caused the ~0.5s black screen.
    // The video "ghost" the hold used to mask is now killed at the source (VideoSurface clears its
    // surface on dispose), so the backdrop no longer needs to outlive the close.
    val viewerOverlayMounted = viewerRequest != null

    // Swipe-down dismiss progress (0f at rest → 1f fully dragged off) reported up from ViewerScreen.
    // The opaque backdrop fades by this amount so the grid shows THROUGH the drag instead of the
    // photo sliding down over black — the swipe-down-only black screen. Other close paths leave this
    // at 0 and are handled by the AnimatedVisibility exit fading over the grid.
    var viewerDismissProgress by remember { mutableFloatStateOf(0f) }
    var viewerDismissing by remember { mutableStateOf(false) }
    var secureOpen by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var holdReturnBackdrop by remember { mutableStateOf(false) }
    var editorOpeningBackdrop by remember { mutableStateOf(false) }
    val viewerOpen = viewerRequest != null && !isEditorOpen

    fun openViewer(items: List<MediaItem>, uri: android.net.Uri, sourceBounds: Rect? = null, onCloseReturn: (() -> Unit)? = null) {
        nextViewerOpenId += 1
        val req = ViewerRequest(nextViewerOpenId, items, uri, sourceBounds = sourceBounds, onCloseReturn = onCloseReturn)
        // Set the rendered request synchronously (not deferred via the LaunchedEffect below) so the
        // viewer's opaque content covers the grid on the very first frame. Otherwise the overlay
        // renders empty for one frame and the grid flashes through during the enter animation.
        activeViewerRequest = req
        viewerRequest = req
        viewerReturnAction = onCloseReturn
        viewerDismissProgress = 0f
        viewerDismissing = false
        viewerSwipeClosing = false
    }
    val onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit = { items, uri, sourceBounds ->
        openViewer(items, uri, sourceBounds)
    }

    LaunchedEffect(externalItem) {
        if (externalItem != null) {
            openViewer(listOf(externalItem), externalItem.uri, sourceBounds = null, onCloseReturn = onExternalClose)
        }
    }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        launch {
            app.mediaAdded.collect { count ->
                snackbarHost.showSnackbar(if (count == 1) "Added 1 new item" else "Added $count new items")
            }
        }
        launch { app.showSnackbar.collect { snackbarHost.showSnackbar(it) } }
    }

    val settings by app.settings.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current



    GalleryBackgroundHost {
        Box(Modifier.fillMaxSize().background(com.gorilla.gallery.ui.theme.DesignTokens.BgBase)) {
            if (!granted) {
                PermissionGate(onRequest = { permissionState.launchMultiplePermissionRequest() })
            } else {
                MainShell(
                    app = app,
                    navController = navController,
                    viewerOpen = viewerOpen,
                    onOpenViewer = onOpenViewer,
                    editorFallbackItem = editorFallbackItem,
                    onEditorClosed = { editorFallbackItem = null },
                )
            }

            // Opaque backdrop shown from open until the close settles, BEHIND the animating viewer.
            // The enter is a ~0.5s fade+scale spring and the exit a quick fade; without this backdrop
            // the translucent viewer would reveal the grid through it during either animation.
            val showViewerBackdrop = ((viewerOverlayMounted || holdReturnBackdrop) && !isEditorOpen) || editorOpeningBackdrop
            if (showViewerBackdrop) {
                val backdropAlpha = if (holdReturnBackdrop) {
                    1f
                } else {
                    (1f - (viewerDismissProgress / 0.28f)).coerceIn(0f, 1f)
                }
                val viewerBackdropColor = if (appColors.isDark) {
                    DesignTokens.BgBase
                } else {
                    Color.Black.copy(alpha = 0.86f)
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .alpha(backdropAlpha)
                        .background(viewerBackdropColor)
                )
            }

            // Opening the editor (which sits opaque just beneath, entering with NO animation) flips
            // this `visible` false, so the viewer fades out QUICKLY (180ms) over it. That's fast
            // enough to read as the photo settling into edit mode — not the slow 0.5s shift we had
            // before, nor the abrupt jump from removing it instantly. The editor underneath is always
            // painted (same cached photo), so there's no gap and the grid never shows. Normal close
            // fades over the BgBase backdrop above instead.
            AnimatedVisibility(
                visible = viewerRequest != null && !isEditorOpen,
                enter = fadeIn(tween(durationMillis = 150, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)),
                exit = if (viewerSwipeClosing) {
                    fadeOut(tween(durationMillis = 80, easing = FastOutLinearInEasing))
                } else {
                    fadeOut(tween(durationMillis = 150, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 0.94f, animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
                }
            ) {
                activeViewerRequest?.let { req ->
                    androidx.compose.runtime.key(req.openId) {
                        fun closeViewer() {
                            val returnAction = viewerReturnAction ?: viewerRequest?.onCloseReturn
                            if (returnAction != null) {
                                holdReturnBackdrop = true
                            }
                            viewerRequest = null
                            viewerReturnAction = null
                            viewerDismissProgress = 0f
                            viewerDismissing = false
                            app.resetPalette()
                            returnAction?.invoke()
                            if (returnAction != null) {
                                scope.launch {
                                    delay(300)
                                    holdReturnBackdrop = false
                                }
                            }
                        }
                        ViewerScreen(
                            app = app,
                            initialItems = req.items,
                            selectedUri = req.currentUri,
                            sourceBounds = (context.applicationContext as com.gorilla.gallery.GalleryApp).itemBounds[req.currentUri] ?: req.sourceBounds,
                            onClose = {
                                viewerSwipeClosing = req.sourceBounds != null
                                closeViewer()
                            },
                            onSwipeDismiss = {
                                viewerSwipeClosing = true
                                closeViewer()
                            },
                            onEdit = { item ->
                                val mediaUri = item.uri
                                editorFallbackItem = item.takeIf { it.trashId != null || it.secureId != null }
                                editorOpeningBackdrop = true
                                viewerRequest = viewerRequest?.copy(currentUri = mediaUri)
                                activeViewerRequest = activeViewerRequest?.copy(currentUri = mediaUri)
                                val route = if (item.isVideo) "videoEditor" else "editor"
                                navController.navigate("$route/${android.net.Uri.encode(mediaUri.toString())}")
                                scope.launch {
                                    delay(220)
                                    editorOpeningBackdrop = false
                                }
                                // takePersistableUriPermission is a main-thread IPC that hitched the
                                // viewer photo at the moment of tapping Edit. The editor reads the
                                // photo via the still-valid session grant, so persist access in the
                                // background — it only matters for cross-session reads.
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        context.contentResolver.takePersistableUriPermission(
                                            mediaUri,
                                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    } catch (e: SecurityException) {
                                        // URI doesn't support persistable permissions — that's fine.
                                    }
                                }
                            },
                            onCurrentItemChanged = { item ->
                                if (viewerRequest?.currentUri != item.uri) {
                                    val updated = viewerRequest?.copy(currentUri = item.uri)
                                    viewerRequest = updated
                                    activeViewerRequest = updated
                                }
                            },
                            isEditorOpen = isEditorOpen,
                            onDismissProgress = { progress ->
                                viewerDismissProgress = progress
                                viewerDismissing = progress > 0.01f
                            },
                            dismissRequested = viewerDismissing,
                            highQualityThumbnails = settings.highQualityThumbnails
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 110.dp),
            )
        }
        }
    }

@Composable
private fun MainShell(
    app: AppViewModel,
    navController: NavHostController,
    viewerOpen: Boolean,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
    editorFallbackItem: MediaItem?,
    onEditorClosed: () -> Unit,
) {
    val contentBackdrop = rememberLayerBackdrop()
    val backStack by navController.currentBackStackEntryAsState()
    
    val current = remember(backStack) {
        var dest = Destination.fromRoute(backStack?.destination?.route)
        if (dest == Destination.Albums) {
            val prev = navController.previousBackStackEntry?.destination?.route
            if (prev?.startsWith(Destination.Search.route) == true) {
                dest = Destination.Search
            }
        }
        dest
    }
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val showNav = backStack?.destination?.route?.let { route ->
        !route.startsWith("editor") && !route.startsWith("videoEditor")
    } ?: true

    CompositionLocalProvider(LocalLiquidGlassContentBackdrop provides contentBackdrop) {
        Box(Modifier.fillMaxSize()) {
            val startPadding by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isLandscape && showNav) 96.dp else 0.dp,
                label = "navStartPadding"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(contentBackdrop)
                    .background(com.gorilla.gallery.ui.theme.DesignTokens.BgBase)
            ) {
                Box(
                    Modifier
                        .padding(start = startPadding)
                        .fillMaxSize()
                ) {
                    GalleryNavHost(
                        navController = navController,
                        app = app,
                        viewerOpen = viewerOpen,
                        onOpenViewer = onOpenViewer,
                        editorFallbackItem = editorFallbackItem,
                        onEditorClosed = onEditorClosed,
                        contentPadding = PaddingValues(
                            bottom = if (isLandscape) 24.dp else 120.dp
                        ),
                    )
                }
            }

            // Glass nav — hidden on editor routes.
            AnimatedVisibility(
                visible = showNav,
                enter = fadeIn(spring(dampingRatio = 0.82f, stiffness = 380f)) +
                    scaleIn(initialScale = 0.98f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)),
                exit = fadeOut(spring(dampingRatio = 0.9f, stiffness = 420f)) +
                    scaleOut(targetScale = 0.98f, animationSpec = spring(dampingRatio = 0.9f, stiffness = 420f)),
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                if (isLandscape) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(start = 20.dp, top = 12.dp, bottom = 12.dp),
                        ) {
                            com.gorilla.gallery.ui.nav.GlassNavigationRail(
                                current = current,
                                onSelect = { dest ->
                                    if (dest.route != current.route) {
                                        if (current == Destination.Albums && dest != Destination.Albums) {
                                            app.collapseAlbums()
                                        }
                                        var shouldNavigate = true
                                        if (current == Destination.Search) {
                                            navController.popBackStack(Destination.Search.route, inclusive = true)
                                            val topRoute = navController.currentDestination?.route
                                            if (topRoute == dest.route) {
                                                shouldNavigate = false
                                            }
                                        } else if (dest == Destination.Albums) {
                                            if (navController.popBackStack(Destination.Albums.route, inclusive = false)) {
                                                shouldNavigate = false
                                            }
                                        }
                                        if (shouldNavigate) {
                                            navController.navigate(dest.route) {
                                                popUpTo(Destination.Timeline.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    ) {
                        // Main Nav Bar (Left)
                        Box(modifier = Modifier.align(Alignment.CenterStart)) {
                            GlassNavigationBar(
                                current = current,
                                onSelect = { dest ->
                                    if (dest.route != current.route) {
                                        if (current == Destination.Albums && dest != Destination.Albums) {
                                            app.collapseAlbums()
                                        }

                                        var shouldNavigate = true
                                        
                                        if (current == Destination.Search) {
                                            navController.popBackStack(Destination.Search.route, inclusive = true)
                                            val topRoute = navController.currentDestination?.route
                                            if (topRoute == dest.route) {
                                                shouldNavigate = false
                                            }
                                        } else if (dest == Destination.Albums) {
                                            if (navController.popBackStack(Destination.Albums.route, inclusive = false)) {
                                                shouldNavigate = false
                                            }
                                        }

                                        if (shouldNavigate) {
                                            navController.navigate(dest.route) {
                                                popUpTo(Destination.Timeline.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // Search Pill (Right)
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            val isSearchSelected = current == Destination.Search
                            val surfaceOpacity = com.gorilla.gallery.ui.theme.LocalTrueLiquidGlassSurfaceOpacity.current
                            TimelineCirclePill(
                                icon = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = if (isSearchSelected) LocalDynamicColors.current.accent.copy(alpha = 0.75f) else DesignTokens.TextSecondary,
                                surfaceAlphaOverride = surfaceOpacity,
                                size = 64.dp,
                                iconSize = 24.dp,
                                isSelected = isSearchSelected,
                                onClick = {
                                    if (!isSearchSelected) {
                                        if (current == Destination.Albums) {
                                            app.collapseAlbums()
                                        }
                                        navController.navigate("${Destination.Search.route}?autoFocus=true") {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                        }
                    }
                }
            } // AnimatedVisibility
        } // Box
    } // CompositionLocalProvider
} // MainShell


@Composable
private fun TimelineCirclePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = DesignTokens.TextPrimary,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp,
    isSelected: Boolean = false,
    surfaceAlphaOverride: Float? = null,
) {
    val haptic = rememberHaptic()
    GlassIconPill(
        icon = icon,
        contentDescription = contentDescription,
        onClick = { haptic(); onClick() },
        size = size,
        iconSize = iconSize,
        tint = tint,
        isSelected = isSelected,
        backdrop = LocalLiquidGlassContentBackdrop.current,
        surfaceAlphaOverride = surfaceAlphaOverride,
    )
}
