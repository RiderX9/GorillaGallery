package com.gorilla.gallery.ui.screens.viewer

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.ComponentActivity
import com.gorilla.gallery.data.repo.TrashPrep
import com.gorilla.gallery.ui.components.GlassAlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import me.saket.telephoto.zoomable.ZoomSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.gorilla.gallery.GalleryApp
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.shareItems
import com.gorilla.gallery.ui.theme.CapsuleShape
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.gallery.ui.theme.kernelSuGlassBackdrop
import com.gorilla.gallery.ui.theme.pressScale
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.SystemClock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Full-screen viewer overlay: a HorizontalPager with per-page pinch-zoom (1–5x),
 * double-tap 2x, single-tap UI toggle, drag-down-to-dismiss, swipe-up info, and a
 * reactive background driven by the current photo's palette.
 */
@Composable
fun ViewerScreen(
    app: AppViewModel,
    initialItems: List<MediaItem>,
    selectedUri: android.net.Uri,
    sourceBounds: Rect? = null,
    onClose: () -> Unit,
    onSwipeDismiss: () -> Unit,
    onEdit: (MediaItem) -> Unit,
    onCurrentItemChanged: (MediaItem) -> Unit = {},
    isEditorOpen: Boolean = false,
    onDismissProgress: (Float) -> Unit = {},
    dismissRequested: Boolean = false,
    highQualityThumbnails: Boolean = false,
) {
    var items by remember(initialItems) { mutableStateOf(initialItems) }
    if (items.isEmpty()) { onClose(); return }
    val startIndex = remember(items) {
        items.indexOfFirst { it.uri == selectedUri }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) { items.size }
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenHeightPx = with(density) {
        configuration.screenHeightDp.dp.toPx()
    }
    val screenWidthPx = with(density) {
        configuration.screenWidthDp.dp.toPx()
    }

    var uiVisible by remember { mutableStateOf(true) }
    var pageScale by remember { mutableFloatStateOf(1f) }
    val coroutineScope = rememberCoroutineScope()
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val dragReset = remember { Animatable(0f) }
    val dragXReset = remember { Animatable(0f) }
    val minimizeProgress = remember { Animatable(0f) }
    val handoffProgress = remember { Animatable(0f) }
    var showInfo by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var showEditMetadata by remember { mutableStateOf(false) }
    var showWallpaperConfirm by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showSecureConfirm by remember { mutableStateOf(false) }
    
    var inPip by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    DisposableEffect(activity) {
        val listener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            inPip = info.isInPictureInPictureMode
        }
        activity?.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(listener)
        }
    }

    val folders by app.deviceFolders.collectAsStateWithLifecycle()
    val favOverride = remember { mutableStateMapOf<Long, Boolean>() }
    var showDeleteConfirm by remember { mutableStateOf<MediaItem?>(null) }
    var showTrashDeleteConfirm by remember { mutableStateOf<MediaItem?>(null) }
    var showTrashRestoreConfirm by remember { mutableStateOf<MediaItem?>(null) }
    var activeTrashPrep by remember { mutableStateOf<TrashPrep?>(null) }
    var closeInProgress by remember { mutableStateOf(false) }
    var fullResDecodePaused by remember { mutableStateOf(false) }
    var activeVideoBackdrop by remember { mutableStateOf<com.kyant.backdrop.Backdrop?>(null) }
    val appColors = LocalAppColors.current

    fun dismissToGrid() {
        if (closeInProgress) return
        closeInProgress = true
        coroutineScope.launch {
            dragReset.stop()
            if (sourceBounds != null) {
                handoffProgress.snapTo(0f)
                minimizeProgress.animateTo(
                    1f,
                    tween(durationMillis = 145, easing = FastOutSlowInEasing)
                )
            }
            onSwipeDismiss()
        }
    }

    DisposableEffect(uiVisible, context, appColors.isDark) {
        val window = (context as? ComponentActivity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (window != null && controller != null) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            if (uiVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            controller?.apply {
                show(WindowInsetsCompat.Type.systemBars())
                isAppearanceLightStatusBars = !appColors.isDark
                isAppearanceLightNavigationBars = !appColors.isDark
            }
        }
    }

    val skipConfirmationsAndToasts = com.gorilla.gallery.ui.theme.LocalAppSettings.current.skipConfirmationsAndToasts
    val moveToTrashByDefaultForDelete = com.gorilla.gallery.ui.theme.LocalAppSettings.current.moveToTrashByDefaultForDelete
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val prep = activeTrashPrep
        if (prep != null) {
            coroutineScope.launch {
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    app.container.trashRepository.onTrashConfirmed(prep.mediaIds)
                    app.showSnackbar.tryEmit(if (moveToTrashByDefaultForDelete) "Moved to Trash" else "Deleted permanently")
                    onClose()
                } else {
                    app.container.trashRepository.rollback(prep.trashIds)
                }
                activeTrashPrep = null
            }
        }
    }

    val handleDelete: () -> Unit = {
        val item = items.getOrNull(pagerState.currentPage)
        if (item != null) {
            if (skipConfirmationsAndToasts) {
                app.moveToTrash(listOf(item))
                onClose()
            } else {
                showDeleteConfirm = item
            }
        }
    }

    BackHandler {
        if (showInfo) {
            showInfo = false
        } else if (showOptions) {
            showOptions = false
        } else if (showEditMetadata) {
            showEditMetadata = false
        } else if (showWallpaperConfirm) {
            showWallpaperConfirm = false
        } else if (showFolderPicker) {
            showFolderPicker = false
        } else if (showDeleteConfirm != null) {
            showDeleteConfirm = null
        } else if (showTrashDeleteConfirm != null) {
            showTrashDeleteConfirm = null
        } else if (showTrashRestoreConfirm != null) {
            showTrashRestoreConfirm = null
        } else {
            dismissToGrid()
        }
    }

    val current = items.getOrNull(pagerState.currentPage) ?: return
    val currentUri = current.uri
    // Only fall back to sourceBounds if we're still looking at the initially clicked photo.
    // If they swiped to a new photo and it's off-screen (so itemBounds is missing), we want a centered crossfade (source=null).
    val fallbackBounds = if (currentUri == selectedUri) sourceBounds else null
    val source = (context.applicationContext as com.gorilla.gallery.GalleryApp).itemBounds[currentUri] ?: fallbackBounds
    LaunchedEffect(currentUri) {
        fullResDecodePaused = false
    }
    LaunchedEffect(selectedUri, items) {
        val targetIndex = items.indexOfFirst { it.uri == selectedUri }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetIndex)
        }
    }
    LaunchedEffect(pagerState, items) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                items.getOrNull(page)?.let {
                    app.onPhotoFocused(it)
                    onCurrentItemChanged(it)
                }
                pageScale = 1f
            }
    }

    LaunchedEffect(current.isVideo, activity) {
        if (!current.isVideo && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && activity != null) {
            try {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(false)
                    .build()
                activity.setPictureInPictureParams(params)
            } catch (e: Exception) {}
        }
    }

    val dragProgress = (abs(dragY) / (screenHeightPx * 0.48f)).coerceIn(0f, 1f)
    val dismissProgress = maxOf(dragProgress, handoffProgress.value, minimizeProgress.value)
    val returningToGrid = handoffProgress.value > 0f || minimizeProgress.value > 0f

    // Report drag progress up so the host's opaque backdrop fades in step, letting the grid show
    // through the swipe-down instead of the photo dragging over black.
    LaunchedEffect(dismissProgress) { onDismissProgress(dismissProgress) }

    val dismissChromeAlpha = (1f - (dismissProgress / 0.08f)).coerceIn(0f, 1f)
    val viewerBackdrop = Color.Black.copy(alpha = 0.82f)
    val viewerShade = if (appColors.isDark) {
        Color.Black.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val viewerContentBackdrop = rememberLayerBackdrop()

    CompositionLocalProvider(LocalLiquidGlassContentBackdrop provides viewerContentBackdrop) {
    Box(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(viewerBackdrop.copy(alpha = viewerBackdrop.alpha * (1f - dismissProgress))))
        Box(Modifier.fillMaxSize().background(viewerShade.copy(alpha = viewerShade.alpha * (1f - dismissProgress))))

        val mediaAspect = if (current.displayWidth > 0 && current.displayHeight > 0) {
            current.displayWidth.toFloat() / current.displayHeight.toFloat()
        } else {
            screenWidthPx / screenHeightPx
        }
        val screenAspect = screenWidthPx / screenHeightPx
        val fittedWidth: Float
        val fittedHeight: Float
        if (mediaAspect > screenAspect) {
            fittedWidth = screenWidthPx
            fittedHeight = screenWidthPx / mediaAspect
        } else {
            fittedHeight = screenHeightPx
            fittedWidth = screenHeightPx * mediaAspect
        }
        val fittedCenterX = screenWidthPx / 2f
        val fittedCenterY = screenHeightPx / 2f
        val targetCenterX = source?.center?.x ?: fittedCenterX
        val targetCenterY = source?.center?.y ?: screenHeightPx
        val targetTranslationX = targetCenterX - fittedCenterX
        val targetTranslationY = targetCenterY - fittedCenterY
        val returnProgress = minimizeProgress.value.coerceIn(0f, 1f)
        val dragScale = 1f - dragProgress * 0.11f
        val settledScale = dragScale + (1f - dragScale) * returnProgress
        val dragTranslationX = dragX
        val settledTranslationX = (dragTranslationX * (1f - returnProgress)) + (targetTranslationX * maxOf(handoffProgress.value, returnProgress))
        val dragTranslationY = dragY
        val settledTranslationY = (dragTranslationY * (1f - returnProgress)) + (targetTranslationY * returnProgress)
        val cornerProgress = maxOf(handoffProgress.value, minimizeProgress.value).coerceIn(0f, 1f)
        val cornerRadiusDp = 28f + (6f - 28f) * FastOutSlowInEasing.transform(cornerProgress)
        val fittedWidthDp = with(density) { fittedWidth.toDp() }
        val fittedHeightDp = with(density) { fittedHeight.toDp() }
        val targetWidthDp = source?.let { with(density) { (it.width + 2).toDp() } } ?: fittedWidthDp
        val targetHeightDp = source?.let { with(density) { (it.height + 2).toDp() } } ?: fittedHeightDp
        val mediaWidthDp = lerp(fittedWidthDp, targetWidthDp, returnProgress) * settledScale
        val mediaHeightDp = lerp(fittedHeightDp, targetHeightDp, returnProgress) * settledScale
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(viewerContentBackdrop)
                .pointerInput(pageScale <= 1.01f && !isEditorOpen) {
                    if (pageScale <= 1.01f && !isEditorOpen) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                var isVerticalDrag = false
                                var accumulatedDrag = 0f
                                var lastDragTimeMs = down.uptimeMillis
                                var lastDragDelta = 0f
                                var dragYAccumulator = 0f
                                var dragXAccumulator = 0f
                                var swipeDownOccurred = false
                                var pointerId = down.id
                                
                                val touchSlop = 4f

                                do {
                                    val event = awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)

                                    if (event.changes.count { it.pressed } >= 2) {
                                        dragY = 0f
                                        dragX = 0f
                                        fullResDecodePaused = false
                                        dragYAccumulator = 0f
                                        dragXAccumulator = 0f
                                        isVerticalDrag = false
                                        break
                                    }

                                    val dragEvent = event.changes.firstOrNull { it.id == pointerId }
                                    if (dragEvent == null) break

                                    if (!isVerticalDrag) {
                                        val dx = dragEvent.position.x - down.position.x
                                        val dy = dragEvent.position.y - down.position.y
                                        if (kotlin.math.abs(dy) > touchSlop && kotlin.math.abs(dy) > kotlin.math.abs(dx) * 0.5f) {
                                            isVerticalDrag = true
                                            if (dy > 0f) {
                                                fullResDecodePaused = true
                                            }
                                            dragEvent.consume()
                                            coroutineScope.launch {
                                                dragReset.stop()
                                                dragXReset.stop()
                                                handoffProgress.snapTo(0f)
                                                minimizeProgress.snapTo(0f)
                                            }
                                        } else if (kotlin.math.abs(dx) > touchSlop * 2f && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                            break
                                        }
                                    }

                                    if (isVerticalDrag) {
                                        val deltaY = dragEvent.position.y - dragEvent.previousPosition.y
                                        val deltaX = dragEvent.position.x - dragEvent.previousPosition.x
                                        dragEvent.consume()
                                        accumulatedDrag += deltaY
                                        val elapsedMs = (dragEvent.uptimeMillis - lastDragTimeMs).coerceAtLeast(1L)
                                        val velocityY = if (lastDragTimeMs == down.uptimeMillis) 0f else deltaY / elapsedMs * 1000f
                                        lastDragTimeMs = dragEvent.uptimeMillis
                                        lastDragDelta = velocityY

                                        if (swipeDownOccurred || accumulatedDrag > 0f || dragYAccumulator > 0f) {
                                            swipeDownOccurred = true
                                            val maxDragX = screenWidthPx * 0.80f
                                            val maxDragY = screenHeightPx * 0.80f
                                            dragYAccumulator = (dragYAccumulator + deltaY).coerceIn(-maxDragY, maxDragY)
                                            dragXAccumulator = (dragXAccumulator + deltaX).coerceIn(-maxDragX, maxDragX)
                                            dragY = dragYAccumulator
                                            dragX = dragXAccumulator
                                            fullResDecodePaused = dragYAccumulator > 0f
                                        } else {
                                            if (!swipeDownOccurred && accumulatedDrag < -100f && !showInfo) {
                                                showInfo = true
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (isVerticalDrag) {
                                    val shouldDismiss = dragYAccumulator > screenHeightPx * 0.10f || lastDragDelta > 760f
                                    if (shouldDismiss) {
                                        dismissToGrid()
                                    } else {
                                        coroutineScope.launch {
                                            dragReset.snapTo(dragY)
                                            dragXReset.snapTo(dragX)
                                            launch {
                                                dragXReset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 520f)) {
                                                    dragX = value
                                                }
                                            }
                                            dragReset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 520f)) {
                                                dragY = value
                                            }
                                            fullResDecodePaused = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                pageSpacing = 16.dp,
                userScrollEnabled = pageScale <= 1.01f && !isEditorOpen,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val item = items[page]
                val pageMediaAspect = if (item.displayWidth > 0 && item.displayHeight > 0) {
                    item.displayWidth.toFloat() / item.displayHeight.toFloat()
                } else {
                    screenWidthPx / screenHeightPx
                }
                val pageFittedWidth: Float
                val pageFittedHeight: Float
                if (pageMediaAspect > screenAspect) {
                    pageFittedWidth = screenWidthPx
                    pageFittedHeight = screenWidthPx / pageMediaAspect
                } else {
                    pageFittedHeight = screenHeightPx
                    pageFittedWidth = screenHeightPx * pageMediaAspect
                }
                val pageFittedWidthDp = with(density) { pageFittedWidth.toDp() }
                val pageFittedHeightDp = with(density) { pageFittedHeight.toDp() }

                ZoomablePage(
                    item = item,
                    uiVisible = uiVisible && !showInfo && !showOptions && dismissProgress < 0.02f,
                    isCurrent = page == pagerState.currentPage,
                    isFavorite = favOverride[item.id] ?: item.isFavorite,
                    onFavorite = { app.toggleFavorite(item); favOverride[item.id] = !(favOverride[item.id] ?: item.isFavorite) },
                    onShare = { shareItems(context, listOf(item)) },
                    onDelete = handleDelete,
                    onInfo = { showInfo = true },
                    onMenu = { showOptions = true },
                    onEdit = { onEdit(item) },
                    onTap = { uiVisible = !uiVisible },
                    onHide = { dismissToGrid() },
                    onScaleChange = { if (page == pagerState.currentPage) pageScale = it },
                    isEditorOpen = isEditorOpen,
                    dismissRequested = dismissRequested,
                    refreshTrigger = app.refreshTrigger,
                    pauseFullResDecode = page == pagerState.currentPage && fullResDecodePaused,
                    highQualityThumbnails = highQualityThumbnails,
                    onProvideBackdrop = { activeVideoBackdrop = it },
                    dismissTransform = if (page == pagerState.currentPage) {
                        ViewerDismissTransform(
                            width = mediaWidthDp,
                            height = mediaHeightDp,
                            translationX = settledTranslationX,
                            translationY = settledTranslationY,
                            cornerRadius = cornerRadiusDp.dp,
                            clip = dismissProgress > 0f,
                            alpha = if (returningToGrid) 1f - (minimizeProgress.value * 0.08f) else 1f,
                            crop = dismissProgress > 0.001f,
                        )
                    } else {
                        ViewerDismissTransform(
                            width = pageFittedWidthDp,
                            height = pageFittedHeightDp,
                            translationX = 0f,
                            translationY = 0f,
                            cornerRadius = 0.dp,
                            clip = false,
                            alpha = 1f,
                            crop = false,
                        )
                    },
                )
            }
        }

        // Top glass bar.
        AnimatedVisibility(
            visible = !inPip && uiVisible && (!current.isVideo || !isLandscape || current.trashId != null || current.secureId != null),
            enter = slideInVertically(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) { -it } + fadeIn(animationSpec = tween(durationMillis = 100, easing = LinearEasing)),
            exit = slideOutVertically(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) { -it } + fadeOut(animationSpec = tween(durationMillis = 100, easing = LinearEasing)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                // Also fade with the swipe-down so the top/bottom bars don't linger over the
                // revealed grid while the photo drags away.
                .alpha(dismissChromeAlpha),
        ) {
            TopBar(item = current, onClose = { dismissToGrid() }, onMenu = { showOptions = true }, onInfo = { showInfo = true })
        }

        // Bottom glass bar.
        AnimatedVisibility(
            visible = !inPip && uiVisible && !showOptions && !showInfo && (!current.isVideo || current.trashId != null || current.secureId != null),
            enter = slideInVertically(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(durationMillis = 100, easing = LinearEasing)),
            exit = slideOutVertically(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) { it } + fadeOut(animationSpec = tween(durationMillis = 100, easing = LinearEasing)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                // Also fade with the swipe-down so the top/bottom bars don't linger over the
                // revealed grid while the photo drags away.
                .alpha(dismissChromeAlpha),
        ) {
            if (current.trashId != null || current.secureId != null) {
                InternalFolderBottomBar(
                    isImage = !current.isVideo,
                    restoreLabel = if (current.trashId != null) "Restore" else "Move out",
                    onRestore = {
                        if (current.trashId != null) {
                            if (skipConfirmationsAndToasts) {
                                app.restoreFromTrash(current.trashId)
                                onClose()
                            } else {
                                showTrashRestoreConfirm = current
                            }
                        } else {
                            app.moveOutOfSecure(current)
                            onClose()
                        }
                    },
                    onDelete = {
                        if (current.trashId != null) {
                            if (skipConfirmationsAndToasts) {
                                app.deleteInternalItem(current)
                                onClose()
                            } else {
                                showTrashDeleteConfirm = current
                            }
                        } else {
                            app.deleteInternalItem(current)
                            onClose()
                        }
                    },
                    onEdit = { onEdit(current) },
                    onInfo = { showInfo = true },
                )
            } else {
                BottomBar(
                    isFavorite = favOverride[current.id] ?: current.isFavorite,
                    isImage = !current.isVideo,
                    onFavorite = { app.toggleFavorite(current); favOverride[current.id] = !(favOverride[current.id] ?: current.isFavorite) },
                    onEdit = { onEdit(current) },
                    onShare = { shareItems(context, listOf(current)) },
                    onDelete = handleDelete,
                    onInfo = { showInfo = true },
                )
            }
        }
    }

    if (showInfo) {
        val backdrop = if (current.isVideo) activeVideoBackdrop else null
        CompositionLocalProvider(LocalLiquidGlassContentBackdrop provides (backdrop ?: LocalLiquidGlassContentBackdrop.current)) {
            PhotoInfoSheet(
                app = app,
                item = current,
                onDismiss = { showInfo = false },
                onEditMetadata = { showInfo = false; showEditMetadata = true }
            )
        }
    }

    showDeleteConfirm?.let { item ->
        GlassAlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = "Delete this photo?",
            text = if (moveToTrashByDefaultForDelete) "Move this item to Trash?" else "Permanently delete this item?",
            confirmLabel = "Delete",
            onConfirm = {
                val itemToTrash = showDeleteConfirm
                showDeleteConfirm = null
                if (itemToTrash != null) {
                    coroutineScope.launch {
                        try {
                            app.moveToTrash(listOf(itemToTrash))
                            onClose()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            },
            dismissLabel = "Cancel",
            onDismiss = { showDeleteConfirm = null }
        )
    }

    showTrashRestoreConfirm?.let { item ->
        GlassAlertDialog(
            onDismissRequest = { showTrashRestoreConfirm = null },
            icon = Icons.Rounded.RestoreFromTrash,
            title = "Restore this item?",
            text = "It will be moved back to ${item.relativePath.ifBlank { "your gallery" }}.",
            confirmLabel = "Restore",
            onConfirm = {
                item.trashId?.let { app.restoreFromTrash(it) }
                showTrashRestoreConfirm = null
                onClose()
            },
            dismissLabel = "Cancel",
            onDismiss = { showTrashRestoreConfirm = null }
        )
    }

    showTrashDeleteConfirm?.let { item ->
        GlassAlertDialog(
            onDismissRequest = { showTrashDeleteConfirm = null },
            title = "Permanently Delete?",
            text = "This item will be permanently deleted. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                app.deleteInternalItem(item)
                showTrashDeleteConfirm = null
                onClose()
            },
            dismissLabel = "Cancel",
            onDismiss = { showTrashDeleteConfirm = null }
        )
    }

    if (showOptions) {
        val backdrop = if (current.isVideo) activeVideoBackdrop else null
        CompositionLocalProvider(LocalLiquidGlassContentBackdrop provides (backdrop ?: LocalLiquidGlassContentBackdrop.current)) {
            ViewerOptionsSheet(
                item = current,
                onSecure = {
                    showOptions = false
                    if (current.isSecured) {
                        app.moveOutOfSecure(current)
                        onClose()
                    } else {
                        if (skipConfirmationsAndToasts) {
                            app.moveToSecure(listOf(current))
                            onClose()
                        } else {
                            showSecureConfirm = true
                        }
                    }
                },
                onInfo = { showOptions = false; showInfo = true },
                onEditMetadata = { showOptions = false; showEditMetadata = true },
                onCopy = { showOptions = false; app.copyToClipboard(current) },
                onMove = { showOptions = false; showFolderPicker = true },
                onWallpaper = { showOptions = false; showWallpaperConfirm = true },
                onDismiss = { showOptions = false },
            )
        }
    }

    if (showEditMetadata) {
        EditMetadataDialog(
            currentBaseName = current.displayName.substringBeforeLast('.'),
            currentDateMs = current.dateTakenMs,
            currentLocationName = null,
            onConfirm = { newName, newDateMs, newLat, newLng -> 
                showEditMetadata = false
                val changedDate = if (newDateMs != current.dateTakenMs) newDateMs else null
                android.util.Log.d("GorillaGallery", "UI onConfirm: newName=$newName newDateMs=$newDateMs newLat=$newLat newLng=$newLng")
                app.editMetadata(current, newName, changedDate, newLat, newLng) {
                    val updated = current.copy(
                        displayName = newName,
                        dateTakenMs = newDateMs ?: current.dateTakenMs,
                        dateModifiedSec = if (newDateMs != null) newDateMs / 1000 else current.dateModifiedSec,
                    )
                    items = items.toMutableList().apply {
                        val index = indexOfFirst { it.id == updated.id }
                        if (index >= 0) this[index] = updated
                    }
                }
            },
            onDismiss = { showEditMetadata = false },
        )
    }

    if (showSecureConfirm) {
        GlassAlertDialog(
            onDismissRequest = { showSecureConfirm = false },
            title = "Move to Secure Folder",
            text = "This photo will be moved to your Secure Folder and hidden from the main gallery. " +
                "You can access it anytime by unlocking the Secure Folder in Albums.",
            confirmLabel = "Move",
            onConfirm = {
                showSecureConfirm = false
                app.moveToSecure(listOf(current))
                onClose()
            },
            dismissLabel = "Cancel",
            onDismiss = { showSecureConfirm = false },
        )
    }

    if (showWallpaperConfirm) {
        WallpaperConfirmDialog(
            onConfirm = { showWallpaperConfirm = false; app.setAsWallpaper(current) },
            onDismiss = { showWallpaperConfirm = false },
        )
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            onPick = { path -> showFolderPicker = false; app.moveToAlbum(current, path) },
            onDismiss = { showFolderPicker = false },
        )
    }
    }
}

/** Format the media's DateTaken as (day-of-week, "HH:mm"); null day + "Unknown date" if absent. */
internal fun viewerDateLabels(dateTakenMs: Long): Pair<String?, String> {
    if (dateTakenMs <= 0L) return null to "Unknown date"
    val zdt = Instant.ofEpochMilli(dateTakenMs).atZone(ZoneId.systemDefault())
    val day = zdt.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
    val time = zdt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    return day to time
}

/** Maximum pinch-zoom factor in the viewer. */
private const val MAX_VIEWER_ZOOM = 8f

/**
 * Longest-edge pixel cap for the viewer's full-image decode. 4096 stays within the common GPU
 * texture limit for hardware bitmaps while giving pinch-zoom real detail to resolve.
 */
private val VIEWER_DECODE_CAP = coil.size.Size(4096, 4096)

/**
 * Longest-edge cap for the instant low-res preview shown while the full image decodes. Small enough
 * to decode (heavily subsampled) in tens of milliseconds, eliminating the black flash on big shots.
 */
private const val VIEWER_PREVIEW_SIZE_PX = 640

/** Photos above this size animate dismiss with the preview bitmap to avoid large texture jank. */
private const val HIGH_RES_DISMISS_PIXEL_THRESHOLD = 24_000_000L

/** Minimum release speed (px/s) for a one-finger pan to coast; below this we just stop. */
private const val MIN_FLING_VELOCITY = 80f

private data class PanBounds(val x: Float, val y: Float)

private data class ViewerDismissTransform(
    val width: Dp,
    val height: Dp,
    val translationX: Float,
    val translationY: Float,
    val cornerRadius: Dp,
    val clip: Boolean,
    val alpha: Float,
    val crop: Boolean,
) {
    companion object {
        val None = ViewerDismissTransform(
            width = Dp.Unspecified,
            height = Dp.Unspecified,
            translationX = 0f,
            translationY = 0f,
            cornerRadius = 0.dp,
            clip = false,
            alpha = 1f,
            crop = false,
        )
    }
}

private fun MediaItem.panBounds(containerSize: IntSize, scale: Float): PanBounds {
    if (scale <= 1f || containerSize.width <= 0 || containerSize.height <= 0) return PanBounds(0f, 0f)

    val containerWidth = containerSize.width.toFloat()
    val containerHeight = containerSize.height.toFloat()
    val bitmapWidth = displayWidth.toFloat()
    val bitmapHeight = displayHeight.toFloat()
    val bitmapRatio = if (bitmapHeight > 0f) bitmapWidth / bitmapHeight else 1f
    val containerRatio = containerWidth / containerHeight

    val imageWidth: Float
    val imageHeight: Float
    if (bitmapRatio > containerRatio) {
        imageWidth = containerWidth
        imageHeight = if (bitmapRatio > 0f) containerWidth / bitmapRatio else containerHeight
    } else {
        imageHeight = containerHeight
        imageWidth = containerHeight * bitmapRatio
    }

    return PanBounds(
        x = ((imageWidth * scale) - containerWidth).coerceAtLeast(0f) / 2f,
        y = ((imageHeight * scale) - containerHeight).coerceAtLeast(0f) / 2f,
    )
}

// Both photos AND videos store their UNROTATED pixel dimensions in MediaStore alongside an
// ORIENTATION quarter-turn. A portrait phone video is stored as e.g. 1920x1080 with ORIENTATION=90,
// so we must swap width/height here or the viewer builds a landscape-shaped box and letterboxes the
// (correctly-rotated) video inside it. When ORIENTATION is 0 the stored dimensions are already
// upright and no swap happens.
private val MediaItem.displayWidth: Int
    get() = if (orientation.normalizedQuarterTurn()) height else width

private val MediaItem.displayHeight: Int
    get() = if (orientation.normalizedQuarterTurn()) width else height

private val MediaItem.isHighResolutionPhoto: Boolean
    get() = !isVideo && width.toLong() * height.toLong() >= HIGH_RES_DISMISS_PIXEL_THRESHOLD

private fun Int.normalizedQuarterTurn(): Boolean {
    val normalized = ((this % 360) + 360) % 360
    return normalized == 90 || normalized == 270
}

@Composable
private fun ZoomablePage(
    item: MediaItem,
    uiVisible: Boolean,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onMenu: () -> Unit,
    onEdit: () -> Unit,
    onTap: () -> Unit,
    onHide: () -> Unit,
    onScaleChange: (Float) -> Unit,
    isEditorOpen: Boolean,
    dismissRequested: Boolean,
    refreshTrigger: Int = 0,
    pauseFullResDecode: Boolean = false,
    highQualityThumbnails: Boolean = false,
    onProvideBackdrop: (com.kyant.backdrop.Backdrop) -> Unit = {},
    dismissTransform: ViewerDismissTransform = ViewerDismissTransform(0.dp, 0.dp, 0f, 0f, 0.dp, false, 1f, false)
) {
    val context = LocalContext.current
    val galleryApp = context.applicationContext as GalleryApp

    var fullResLoaded by remember(item.id, item.uri, item.dateModifiedSec, refreshTrigger) {
        mutableStateOf(false)
    }
    var previewBitmap by remember(item.id, item.uri, item.dateModifiedSec, refreshTrigger, highQualityThumbnails) {
        mutableStateOf(
            galleryApp.container.thumbnailRepository.getAnyCached(
                mediaId = item.id,
                cacheVersion = item.dateModifiedSec,
            )
        )
    }
    LaunchedEffect(item.id, item.uri, item.dateModifiedSec, refreshTrigger, highQualityThumbnails) {
        if (previewBitmap == null) {
            previewBitmap = galleryApp.container.thumbnailRepository.load(
                uri = item.uri,
                mediaId = item.id,
                sizePx = VIEWER_PREVIEW_SIZE_PX,
                cacheVersion = item.dateModifiedSec,
            )
        }
    }
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 8f)).apply { contentScale = ContentScale.None }
    val scope = rememberCoroutineScope()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(item.displayWidth, item.displayHeight) {
        if (item.displayWidth > 0 && item.displayHeight > 0) {
            zoomableState.setContentLocation(
                me.saket.telephoto.zoomable.ZoomableContentLocation.scaledInsideAndCenterAligned(
                    androidx.compose.ui.geometry.Size(item.displayWidth.toFloat(), item.displayHeight.toFloat())
                )
            )
        }
    }
    
    
    LaunchedEffect(zoomableState.zoomFraction) {
        val zoom = zoomableState.zoomFraction
        if (zoom != null) {
            onScaleChange(1f + zoom)
        } else {
            onScaleChange(1f)
        }
    }
    
    val dismissingMedia = dismissTransform.crop || dismissTransform.clip
    val previewOnlyDismiss = dismissingMedia && item.isHighResolutionPhoto && previewBitmap != null && !highQualityThumbnails

    val isZoomedIn by remember { androidx.compose.runtime.derivedStateOf { (zoomableState.zoomFraction ?: 0f) > 0.01f } }
    BackHandler(enabled = isCurrent && isZoomedIn) {
        scope.launch {
            zoomableState.resetZoom()
        }
    }

    val zoomModifier = Modifier.zoomable(
        state = zoomableState,
        onClick = { onTap() },
        onDoubleClick = me.saket.telephoto.zoomable.DoubleClickToZoomListener.cycle(2.5f)
    )

    val mediaLayerModifier = if (dismissTransform.width != Dp.Unspecified) {
        Modifier
            .size(dismissTransform.width, dismissTransform.height)
            .graphicsLayer {
                translationX = dismissTransform.translationX
                translationY = dismissTransform.translationY
                transformOrigin = TransformOrigin.Center
                shape = RoundedCornerShape(dismissTransform.cornerRadius)
                clip = dismissTransform.clip
                alpha = dismissTransform.alpha
            }
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center,
    ) {
        if (item.isVideo) {
            VideoSurface(
                item = item,
                onClose = onHide,
                onMenu = onMenu,
                isFavorite = isFavorite,
                onFavorite = onFavorite,
                onShare = onShare,
                onDelete = onDelete,
                onInfo = onInfo,
                onEdit = onEdit,
                controlsVisible = uiVisible && isCurrent,
                onToggleControls = onTap,
                onHideControls = onHide,
                isActive = isCurrent,
                dismissRequested = dismissRequested,
                cropToFill = dismissTransform.crop,
                isDismissingMedia = dismissingMedia,
                previewBitmap = previewBitmap,
                highQualityThumbnails = highQualityThumbnails,
                onProvideBackdrop = onProvideBackdrop,
                modifier = Modifier.fillMaxSize(),
                videoModifier = mediaLayerModifier,
                zoomModifier = if (!isEditorOpen && !dismissingMedia) zoomModifier else Modifier
            )
        } else {
            Box(Modifier.fillMaxSize().then(if (!isEditorOpen && !dismissingMedia) zoomModifier else Modifier), contentAlignment = Alignment.Center) {
                // Reuse the grid thumbnail underneath so huge photos never open on a black frame.
                previewBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = if (dismissTransform.crop) ContentScale.Crop else ContentScale.Fit,
                        modifier = mediaLayerModifier,
                    )
                }
                if (!previewOnlyDismiss) {
                    // Full-resolution image on top — draws over the preview once decoded.
                    @OptIn(com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi::class)
                    com.bumptech.glide.integration.compose.GlideImage(
                        model = if (pauseFullResDecode && !fullResLoaded) null else item.uri,
                        contentDescription = item.displayName,
                        contentScale = if (dismissTransform.crop) ContentScale.Crop else ContentScale.Fit,
                        modifier = mediaLayerModifier
                    ) {
                        it.addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                            override fun onLoadFailed(
                                e: com.bumptech.glide.load.engine.GlideException?,
                                m: Any?,
                                t: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                fullResLoaded = false
                                return false
                            }

                            override fun onResourceReady(
                                resource: android.graphics.drawable.Drawable,
                                model: Any,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                fullResLoaded = true
                                return false
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(item: MediaItem, onClose: () -> Unit, onMenu: () -> Unit, onInfo: () -> Unit) {
    val pillShape = RoundedCornerShape(percent = 50)
    val viewerTextPrimary = viewerChromeContentColor()
    val viewerTextSecondary = viewerChromeSecondaryColor()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        val (day, time) = remember(item.dateTakenMs) { viewerDateLabels(item.dateTakenMs) }
        
        if (isLandscape) {
            androidx.compose.runtime.CompositionLocalProvider(com.gorilla.gallery.ui.theme.LocalAppColors provides com.gorilla.gallery.ui.theme.DarkAppColors) {
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .wrapContentWidth()
                        .clip(pillShape)
                        .pressScale(scale = 0.94f)
                        .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) }
                        .kernelSuGlassBackdrop(
                            backdrop = LocalLiquidGlassContentBackdrop.current,
                            shape = pillShape
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight().padding(start = 8.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = viewerTextPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (day != null) {
                                Text(
                                    day,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = viewerTextPrimary,
                                )
                            }
                            Text(
                                time,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    fontSize = 10.5.sp
                                ),
                                color = viewerTextSecondary,
                            )
                        }
                    }
                }
            }
        } else {
            // Back chevron — 48.dp glass circle.
            CirclePill(pillShape, onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = viewerTextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
    
            // Centre — day of week + time from DateTaken.
            androidx.compose.runtime.CompositionLocalProvider(com.gorilla.gallery.ui.theme.LocalAppColors provides com.gorilla.gallery.ui.theme.DarkAppColors) {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .pressScale(scale = 0.94f)
                        .kernelSuGlassBackdrop(
                            backdrop = LocalLiquidGlassContentBackdrop.current,
                            shape = pillShape
                        )
                ) {
                    Column(
                        Modifier
                            .pointerInput(Unit) { detectTapGestures(onTap = { onInfo() }) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (day != null) {
                            Text(
                                day,
                                style = MaterialTheme.typography.titleSmall,
                                color = viewerTextPrimary,
                            )
                        }
                        Text(
                            time,
                            style = MaterialTheme.typography.labelMedium,
                            color = viewerTextSecondary,
                        )
                    }
                }
            }
        }

        // Three-dot menu — 48.dp glass circle.
        CirclePill(pillShape, onClick = onMenu) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "More",
                tint = viewerTextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun CirclePill(
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(com.gorilla.gallery.ui.theme.LocalAppColors provides com.gorilla.gallery.ui.theme.DarkAppColors) {
        Box(
            modifier = Modifier
                .pressScale(scale = 0.88f)
                .kernelSuGlassBackdrop(
                    backdrop = LocalLiquidGlassContentBackdrop.current,
                    shape = shape
                )
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}

@Composable
private fun BottomBar(
    isFavorite: Boolean,
    isImage: Boolean,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val iconTint = viewerChromeContentColor()
    androidx.compose.runtime.CompositionLocalProvider(com.gorilla.gallery.ui.theme.LocalAppColors provides com.gorilla.gallery.ui.theme.DarkAppColors) {
        Box(
            modifier = Modifier.kernelSuGlassBackdrop(
                backdrop = LocalLiquidGlassContentBackdrop.current,
                shape = CapsuleShape
            )
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewerActionIcon(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) accent else iconTint,
                    )
                }
                if (isImage) {
                    ViewerActionIcon(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = iconTint) }
                }
                ViewerActionIcon(onClick = onShare) { Icon(Icons.Rounded.Share, contentDescription = "Share", tint = iconTint) }
                ViewerActionIcon(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = iconTint) }
                ViewerActionIcon(onClick = onInfo) { Icon(Icons.Rounded.Info, contentDescription = "Info", tint = iconTint) }
            }
        }
    }
}

@Composable
private fun InternalFolderBottomBar(
    isImage: Boolean,
    restoreLabel: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onInfo: () -> Unit,
) {
    val iconTint = viewerChromeContentColor()
    androidx.compose.runtime.CompositionLocalProvider(com.gorilla.gallery.ui.theme.LocalAppColors provides com.gorilla.gallery.ui.theme.DarkAppColors) {
        Box(
            modifier = Modifier.kernelSuGlassBackdrop(
                backdrop = LocalLiquidGlassContentBackdrop.current,
                shape = CapsuleShape
            )
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewerActionIcon(onClick = onRestore) {
                    Icon(Icons.Rounded.RestoreFromTrash, contentDescription = restoreLabel, tint = iconTint)
                }
                ViewerActionIcon(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = iconTint)
                }
                if (isImage) {
                    ViewerActionIcon(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = iconTint)
                    }
                }
                ViewerActionIcon(onClick = onInfo) {
                    Icon(Icons.Rounded.Info, contentDescription = "Info", tint = iconTint)
                }
            }
        }
    }
}

@Composable
internal fun viewerChromeColor(): Color {
    return if (com.gorilla.gallery.ui.theme.LocalAppColors.current.isDark) {
        Color(0xFF1E1E2A).copy(alpha = 0.72f)
    } else {
        Color.Black.copy(alpha = 0.34f)
    }
}

@Composable
internal fun viewerChromeSurfaceColor(): Color {
    return viewerChromeColor()
}

@Composable
internal fun viewerChromeTintAlpha(): Float {
    return 0.07f
}

@Composable
internal fun viewerChromeSaturation(): Float {
    return 1.55f
}

@Composable
internal fun viewerChromeContentColor(): Color {
    return Color.White
}

@Composable
internal fun viewerChromeSecondaryColor(): Color {
    return Color.White.copy(alpha = 0.72f)
}

@Composable
private fun ViewerActionIcon(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.pressScale(scale = 0.88f),
        content = content,
    )
}

