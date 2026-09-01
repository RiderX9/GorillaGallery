@file:android.annotation.SuppressLint("UnsafeOptInUsageError")
package com.gorilla.gallery.ui.screens.videoeditor

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.FilteringMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gorilla.gallery.R
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.CropAspect
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.formatDuration
import com.gorilla.gallery.ui.screens.editor.CropOverlay
import com.gorilla.gallery.ui.screens.editor.pressAndHoldOriginalPreview
import com.gorilla.gallery.ui.screens.editor.snapToAspect
import com.gorilla.gallery.ui.theme.CapsuleShape
import com.gorilla.gallery.ui.theme.GlassDepth
import com.gorilla.gallery.ui.theme.kernelSuGlassBackdrop
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import kotlin.math.abs

private enum class VideoEditorMode(val label: String, val icon: ImageVector) {
    TRIM("Video", Icons.Rounded.ContentCut),
    AUDIO("Audio", Icons.AutoMirrored.Rounded.VolumeUp),
    FILTERS("Filters", Icons.Rounded.FilterVintage),
    CROP("Crop", Icons.Rounded.Crop),
}

private enum class VideoCropPreset(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT("4:5", 4f / 5f),
    STORY("9:16", 9f / 16f),
    WIDE("16:9", 16f / 9f),
}

private fun combineCrops(absolute: androidx.compose.ui.geometry.Rect?, relative: androidx.compose.ui.geometry.Rect?): androidx.compose.ui.geometry.Rect? {
    if (absolute == null) return relative
    if (relative == null) return absolute
    val newLeft = absolute.left + relative.left * absolute.width
    val newTop = absolute.top + relative.top * absolute.height
    val newRight = absolute.left + relative.right * absolute.width
    val newBottom = absolute.top + relative.bottom * absolute.height
    return androidx.compose.ui.geometry.Rect(newLeft, newTop, newRight, newBottom)
}

private val VideoCropPreset.resizeMode: Int
    get() = if (this == VideoCropPreset.ORIGINAL) {
        AspectRatioFrameLayout.RESIZE_MODE_FIT
    } else {
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

private val VideoCropPreset.cropAspect: CropAspect
    get() = when (this) {
        VideoCropPreset.ORIGINAL -> CropAspect.ORIGINAL
        VideoCropPreset.FREE -> CropAspect.FREE
        VideoCropPreset.SQUARE -> CropAspect.SQUARE
        VideoCropPreset.PORTRAIT -> CropAspect.PORTRAIT
        VideoCropPreset.STORY -> CropAspect.STORY
        VideoCropPreset.WIDE -> CropAspect.WIDE
    }

private enum class VideoFilterPreset(val label: String, val overlay: Color) {
    ORIGINAL("Original", Color.Transparent),
    VIVID("Vivid", Color(0xFF7CFFB2).copy(alpha = 0.08f)),
    WARM("Warm", Color(0xFFFFA552).copy(alpha = 0.12f)),
    COOL("Cool", Color(0xFF64B5FF).copy(alpha = 0.12f)),
    FADE("Fade", Color.White.copy(alpha = 0.10f)),
    DRAMATIC("Dramatic", Color.Black.copy(alpha = 0.16f)),
}

@OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(
    app: AppViewModel,
    item: MediaItem,
    onClose: () -> Unit,
    vm: VideoEditorViewModel = viewModel(factory = VideoEditorViewModel.Factory),
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val haptic = rememberHaptic()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current
    val chromeText = appColors.textPrimary
    val chromeTextSecondary = appColors.textSecondary
    val videoFrameBackdrop = rememberLayerBackdrop()
    val durationMs = item.durationMs.coerceAtLeast(1L)
    var trimStartMs by remember(item.uri) { mutableLongStateOf(0L) }
    var trimEndMs by remember(item.uri) { mutableLongStateOf(durationMs) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(VideoEditorMode.TRIM) }
    var openMode by remember { mutableStateOf<VideoEditorMode?>(VideoEditorMode.TRIM) }
    var appliedCropPreset by remember { mutableStateOf(VideoCropPreset.ORIGINAL) }
    var userRotationDegrees by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val appliedCropRect by vm.appliedCropRect.collectAsStateWithLifecycle()
    var draftCropPreset by remember { mutableStateOf(VideoCropPreset.ORIGINAL) }
    var draftCropRect by remember { mutableStateOf<Rect?>(null) }
    var filterPreset by remember { mutableStateOf(VideoFilterPreset.ORIGINAL) }
    var replacementAudioUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showOriginalPreview by remember { mutableStateOf(false) }
    var audioStartMs by remember { mutableLongStateOf(0L) }
    var draftAudioStartMs by remember { mutableLongStateOf(0L) }
    var previewAudioStartMs by remember { mutableLongStateOf(0L) }
    var audioDurationMs by remember { mutableLongStateOf(0L) }
    
    data class VideoEditState(
        val trimStartMs: Long,
        val trimEndMs: Long,
        val appliedCropPreset: VideoCropPreset,
        val appliedCropRect: Rect?,
        val filterPreset: VideoFilterPreset,
        val replacementAudioUri: android.net.Uri?,
        val audioStartMs: Long,
        val muted: Boolean,
    )

    val editUndoStack = remember { androidx.compose.runtime.mutableStateListOf<VideoEditState>() }
    val editRedoStack = remember { androidx.compose.runtime.mutableStateListOf<VideoEditState>() }

    val saveUndoState = {
        editUndoStack.add(
            VideoEditState(
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                appliedCropPreset = appliedCropPreset,
                appliedCropRect = appliedCropRect,
                filterPreset = filterPreset,
                replacementAudioUri = replacementAudioUri,
                audioStartMs = audioStartMs,
                muted = muted,
            )
        )
        if (editUndoStack.size > 50) editUndoStack.removeAt(0)
        editRedoStack.clear()
    }

    val undoFilter = {
        editRedoStack.add(
            VideoEditState(
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                appliedCropPreset = appliedCropPreset,
                appliedCropRect = appliedCropRect,
                filterPreset = filterPreset,
                replacementAudioUri = replacementAudioUri,
                audioStartMs = audioStartMs,
                muted = muted,
            )
        )
        val currentStateReset = VideoEditState(
            trimStartMs = trimStartMs,
            trimEndMs = trimEndMs,
            appliedCropPreset = appliedCropPreset,
            appliedCropRect = appliedCropRect,
            filterPreset = VideoFilterPreset.ORIGINAL,
            replacementAudioUri = replacementAudioUri,
            audioStartMs = audioStartMs,
            muted = muted,
        )
        while (editUndoStack.isNotEmpty()) {
            val last = editUndoStack.last()
            if (last.copy(filterPreset = VideoFilterPreset.ORIGINAL) == currentStateReset) {
                editUndoStack.removeAt(editUndoStack.size - 1)
            } else {
                break
            }
        }
        filterPreset = VideoFilterPreset.ORIGINAL
        haptic()
    }

    val audioPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            saveUndoState()
            replacementAudioUri = uri
            audioStartMs = 0L
            draftAudioStartMs = 0L
            previewAudioStartMs = 0L
            audioDurationMs = 0L
            muted = false
        }
    }

    val cropMenuOpen = openMode == VideoEditorMode.CROP
    val previewCropPreset = if (cropMenuOpen) draftCropPreset else appliedCropPreset
    val previewResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL

    LaunchedEffect(item.uri) {
        appliedCropPreset = VideoCropPreset.ORIGINAL
        vm.applyCrop(null)
        draftCropPreset = VideoCropPreset.ORIGINAL
        draftCropRect = null
        showOriginalPreview = false
    }

    LaunchedEffect(openMode) {
        showOriginalPreview = false
    }

    val player = remember(item.uri) {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            playWhenReady = false
        }
    }

    var playerVideoAspect by remember { mutableStateOf<Float?>(null) }
    val fallbackVideoAspect = run {
        val isRotated = item.orientation == 90 || item.orientation == 270
        val rawVideoW = if (isRotated) item.height.toFloat() else item.width.toFloat()
        val rawVideoH = if (isRotated) item.width.toFloat() else item.height.toFloat()
        (rawVideoW / rawVideoH).takeIf { !it.isNaN() && it > 0f } ?: 1f
    }
    val currentVideoAspect = playerVideoAspect ?: fallbackVideoAspect

    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val isRotated = videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270
                    val w = if (isRotated) videoSize.height.toFloat() else videoSize.width.toFloat()
                    val h = if (isRotated) videoSize.width.toFloat() else videoSize.height.toFloat()
                    playerVideoAspect = (w * videoSize.pixelWidthHeightRatio) / h
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.seekTo(trimStartMs)
                    player.pause()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.clearVideoSurface()
            player.release()
        }
    }

    LaunchedEffect(item.uri, replacementAudioUri, previewAudioStartMs) {
        val wasPlaying = player.isPlaying
        val restorePosition = player.currentPosition.coerceIn(0L, durationMs)
        player.setMediaSource(
            buildPreviewMediaSource(
                context = context,
                videoUri = item.uri,
                replacementAudioUri = replacementAudioUri,
                audioStartMs = previewAudioStartMs,
            ),
        )
        player.prepare()
        player.seekTo(restorePosition)
        if (wasPlaying) {
            player.play()
        }
    }

    LaunchedEffect(replacementAudioUri) {
        audioDurationMs = replacementAudioUri?.let { uri ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                readMediaDurationMs(context, uri)
            }
        } ?: 0L
    }

    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(trimStartMs, trimEndMs, audioStartMs) {
        if (player.currentPosition < trimStartMs || player.currentPosition > trimEndMs) {
            player.seekTo(trimStartMs)
        }
    }

    LaunchedEffect(player, trimStartMs, trimEndMs) {
        while (true) {
            positionMs = player.currentPosition.coerceIn(0L, durationMs)
            if (player.isPlaying && positionMs >= trimEndMs) {
                player.seekTo(trimStartMs)
                player.pause()
            }
            kotlinx.coroutines.delay(160)
        }
    }

    val cropVerticalPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = 0.dp,
        label = "cropPadding"
    )

    val togglePlayback = {
        haptic()
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.currentPosition !in trimStartMs until trimEndMs) {
                player.seekTo(trimStartMs)
            }
            player.play()
        }
    }

    var showSaveOptions by remember { mutableStateOf(false) }
    var confirmOverwrite by remember { mutableStateOf(false) }

    val saveEditedVideo = {
        haptic()
        showSaveOptions = true
    }

    CompositionLocalProvider(LocalLiquidGlassContentBackdrop provides videoFrameBackdrop) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(1f)
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val undoRedoPill = @Composable {
                com.gorilla.gallery.ui.screens.editor.PhotoUndoRedoPill(
                    canUndo = editUndoStack.isNotEmpty() || filterPreset != VideoFilterPreset.ORIGINAL,
                    canRedo = editRedoStack.isNotEmpty(),
                    onUndo = {
                        if (openMode == VideoEditorMode.FILTERS && filterPreset != VideoFilterPreset.ORIGINAL) {
                            undoFilter()
                        } else if (editUndoStack.isNotEmpty()) {
                            val previous = editUndoStack.removeAt(editUndoStack.size - 1)
                            editRedoStack.add(
                                VideoEditState(
                                    trimStartMs = trimStartMs,
                                    trimEndMs = trimEndMs,
                                    appliedCropPreset = appliedCropPreset,
                                    appliedCropRect = appliedCropRect,
                                    filterPreset = filterPreset,
                                    replacementAudioUri = replacementAudioUri,
                                    audioStartMs = audioStartMs,
                                    muted = muted,
                                )
                            )
                            trimStartMs = previous.trimStartMs
                            trimEndMs = previous.trimEndMs
                            appliedCropPreset = previous.appliedCropPreset
                            vm.applyCrop(previous.appliedCropRect)
                            filterPreset = previous.filterPreset
                            replacementAudioUri = previous.replacementAudioUri
                            audioStartMs = previous.audioStartMs
                            muted = previous.muted
                            player.seekTo(trimStartMs)
                            haptic()
                        } else if (filterPreset != VideoFilterPreset.ORIGINAL) {
                            undoFilter()
                        }
                    },
                    onRedo = {
                        if (editRedoStack.isNotEmpty()) {
                            val next = editRedoStack.removeAt(editRedoStack.size - 1)
                            editUndoStack.add(
                                VideoEditState(
                                    trimStartMs = trimStartMs,
                                    trimEndMs = trimEndMs,
                                    appliedCropPreset = appliedCropPreset,
                                    appliedCropRect = appliedCropRect,
                                    filterPreset = filterPreset,
                                    replacementAudioUri = replacementAudioUri,
                                    audioStartMs = audioStartMs,
                                    muted = muted,
                                )
                            )
                            trimStartMs = next.trimStartMs
                            trimEndMs = next.trimEndMs
                            appliedCropPreset = next.appliedCropPreset
                            vm.applyCrop(next.appliedCropRect)
                            filterPreset = next.filterPreset
                            replacementAudioUri = next.replacementAudioUri
                            audioStartMs = next.audioStartMs
                            muted = next.muted
                            player.seekTo(trimStartMs)
                            haptic()
                        }
                    },
                )
            }
            if (isLandscape) {
                AppleEditorTopChrome(
                    saving = saving,
                    backdrop = videoFrameBackdrop,
                    onCancel = onClose,
                    onDone = saveEditedVideo,
                    centerContent = undoRedoPill,
                )
            } else {
                AppleEditorTopChrome(
                    saving = saving,
                    backdrop = videoFrameBackdrop,
                    onCancel = onClose,
                    onDone = saveEditedVideo,
                )
                undoRedoPill()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .padding(
                    start = if (isLandscape) 120.dp else 32.dp,
                    end = if (isLandscape) 120.dp else 32.dp,
                    top = if (isLandscape) 32.dp else 120.dp,
                    bottom = if (isLandscape) 32.dp else 220.dp
                ),
            contentAlignment = Alignment.Center
        ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(vertical = cropVerticalPadding)
                .layerBackdrop(videoFrameBackdrop),
            contentAlignment = Alignment.Center
        ) {
            val widthDp = maxWidth.value.coerceAtLeast(1f)
            val heightDp = maxHeight.value.coerceAtLeast(1f)
            
            val isRotated = item.orientation == 90 || item.orientation == 270
            val rawVideoW = if (isRotated) item.height.toFloat() else item.width.toFloat()
            val rawVideoH = if (isRotated) item.width.toFloat() else item.height.toFloat()
            val actualVideoAspect = playerVideoAspect ?: (rawVideoW / rawVideoH).takeIf { !it.isNaN() && it > 0f } ?: (widthDp / heightDp)
            val videoAspect = if (userRotationDegrees % 180 != 0) 1f / actualVideoAspect else actualVideoAspect
            
            val containerRatio = widthDp / heightDp
            val videoWidthDp: Float
            val videoHeightDp: Float
            if (videoAspect > containerRatio) {
                videoWidthDp = widthDp
                videoHeightDp = widthDp / videoAspect
            } else {
                videoHeightDp = heightDp
                videoWidthDp = heightDp * videoAspect
            }
            
            val isAppliedPreview = !showOriginalPreview &&
                appliedCropPreset != VideoCropPreset.ORIGINAL
            
            val rect = if (isAppliedPreview) {
                appliedCropRect ?: defaultCropRect(appliedCropPreset, videoAspect)
            } else {
                Rect(0f, 0f, 1f, 1f)
            }
            
            val cropWindowWidthDp = rect.width.coerceAtLeast(0.001f) * videoWidthDp
            val cropWindowHeightDp = rect.height.coerceAtLeast(0.001f) * videoHeightDp
            val appliedPreviewScale = if (isAppliedPreview) {
                minOf(
                    widthDp / cropWindowWidthDp.coerceAtLeast(1f),
                    heightDp / cropWindowHeightDp.coerceAtLeast(1f)
                )
            } else {
                1f
            }
            val previewWidthDp = if (isAppliedPreview) cropWindowWidthDp * appliedPreviewScale else videoWidthDp
            val previewHeightDp = if (isAppliedPreview) cropWindowHeightDp * appliedPreviewScale else videoHeightDp
            val scaledVideoWidthDp = videoWidthDp * appliedPreviewScale
            val scaledVideoHeightDp = videoHeightDp * appliedPreviewScale
            val scaledVideoOffsetXDp = if (isAppliedPreview) -rect.left * scaledVideoWidthDp else 0f
            val scaledVideoOffsetYDp = if (isAppliedPreview) -rect.top * scaledVideoHeightDp else 0f

            var previewScale by remember { mutableFloatStateOf(1f) }
            var previewOffset by remember { mutableStateOf(Offset.Zero) }

            LaunchedEffect(item.uri, openMode) {
                previewScale = 1f
                previewOffset = Offset.Zero
            }

            Box(
                modifier = (if (openMode != VideoEditorMode.CROP) {
                    Modifier.pressAndHoldOriginalPreview { showOriginalPreview = it }
                } else {
                    Modifier
                })
                .pointerInput(openMode) {
                    if (openMode != VideoEditorMode.CROP) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            previewScale = (previewScale * zoom).coerceIn(1f, 10f)
                            if (previewScale > 1f) {
                                val maxX = ((previewWidthDp * previewScale) - widthDp).coerceAtLeast(0f) / 2f
                                val maxY = ((previewHeightDp * previewScale) - heightDp).coerceAtLeast(0f) / 2f
                                previewOffset = Offset(
                                    x = (previewOffset.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (previewOffset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            } else {
                                previewOffset = Offset.Zero
                            }
                        }
                    }
                }
                .pointerInput(openMode) {
                    if (openMode != VideoEditorMode.CROP) {
                        detectTapGestures(
                            onDoubleTap = {
                                previewScale = 1f
                                previewOffset = Offset.Zero
                            }
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = previewScale
                    scaleY = previewScale
                    translationX = previewOffset.x
                    translationY = previewOffset.y
                },
                contentAlignment = Alignment.TopStart,
            ) {
                Layout(
                    content = {
                        val scaledPlayerWidthDp = if (userRotationDegrees % 180 != 0) scaledVideoHeightDp else scaledVideoWidthDp
                        val scaledPlayerHeightDp = if (userRotationDegrees % 180 != 0) scaledVideoWidthDp else scaledVideoHeightDp
                        Box(
                            modifier = Modifier
                                .width(scaledVideoWidthDp.dp)
                                .height(scaledVideoHeightDp.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier
                                .requiredSize(scaledPlayerWidthDp.dp, scaledPlayerHeightDp.dp)
                                .graphicsLayer { rotationZ = userRotationDegrees.toFloat() }) {
                                VideoPlayerView(
                                    player = player,
                                    resizeMode = previewResizeMode,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                VideoFilterOverlay(if (showOriginalPreview) VideoFilterPreset.ORIGINAL else filterPreset)
                            }
                        }
                    },
                    modifier = Modifier
                        .width(previewWidthDp.dp)
                        .height(previewHeightDp.dp)
                        .clipToBounds(),
                ) { measurables, _ ->
                    val viewportWidthPx = previewWidthDp.dp.roundToPx().coerceAtLeast(1)
                    val viewportHeightPx = previewHeightDp.dp.roundToPx().coerceAtLeast(1)
                    val videoWidthPx = scaledVideoWidthDp.dp.roundToPx().coerceAtLeast(1)
                    val videoHeightPx = scaledVideoHeightDp.dp.roundToPx().coerceAtLeast(1)
                    val offsetXPx = scaledVideoOffsetXDp.dp.roundToPx()
                    val offsetYPx = scaledVideoOffsetYDp.dp.roundToPx()
                    val placeable = measurables.first().measure(
                        androidx.compose.ui.unit.Constraints.fixed(videoWidthPx, videoHeightPx)
                    )
                    layout(viewportWidthPx, viewportHeightPx) {
                        placeable.place(offsetXPx, offsetYPx)
                    }
                }

                if (!showOriginalPreview && cropMenuOpen && draftCropPreset != VideoCropPreset.ORIGINAL) {
                    Box(
                        Modifier
                            .width(previewWidthDp.dp)
                            .height(previewHeightDp.dp)
                    ) {
                        CropSelectionOverlay(
                            preset = draftCropPreset,
                            cropRect = draftCropRect,
                            onCropRectChange = { draftCropRect = it },
                        )
                    }
                }
            }
        }
        if (!isPlaying) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.38f))
                    .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                    .pressScale(scale = 0.9f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = togglePlayback,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = if (isLandscape) 88.dp else 28.dp,
                    end = if (isLandscape) 88.dp else 28.dp,
                    top = 10.dp,
                    bottom = if (isLandscape) 4.dp else 10.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VideoEditorPanel(
                shape = RoundedCornerShape(28.dp),
                backdrop = videoFrameBackdrop,
                modifier = Modifier.then(if (isLandscape) Modifier else Modifier.fillMaxWidth()).widthIn(max = if (isLandscape) 438.dp else 520.dp),
            ) {
                Column(
                    Modifier.padding(top = 10.dp, bottom = if (isLandscape) 10.dp else 14.dp),
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = openMode != null,
                        enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) + androidx.compose.animation.fadeOut()
                    ) {
                        val visibleMode = openMode ?: mode
                        Column(
                            Modifier.padding(horizontal = 10.dp).then(if (isLandscape) Modifier else Modifier.padding(bottom = 10.dp))
                                .then(if (isLandscape && visibleMode == VideoEditorMode.CROP) Modifier.width(IntrinsicSize.Max) else Modifier),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                        when (visibleMode) {
                        VideoEditorMode.TRIM -> {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    formatDuration(trimStartMs),
                                    color = LocalDynamicColors.current.accent,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    formatDuration(trimEndMs),
                                    color = chromeTextSecondary.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }

                            AppleTrimTimeline(
                                isPlaying = isPlaying,
                                muted = muted,
                                durationMs = durationMs,
                                startMs = trimStartMs,
                                endMs = trimEndMs,
                                playheadMs = positionMs,
                                onTogglePlayback = togglePlayback,
                                onToggleMuted = {
                                    saveUndoState()
                                    muted = !muted
                                },
                                onStartChange = { value ->
                                    if (trimStartMs != value) saveUndoState()
                                    trimStartMs = value.coerceIn(0L, trimEndMs - 500L)
                                    player.seekTo(trimStartMs)
                                },
                                onEndChange = { value ->
                                    if (trimEndMs != value) saveUndoState()
                                    trimEndMs = value.coerceIn(trimStartMs + 500L, durationMs)
                                    player.seekTo((trimEndMs - 100L).coerceAtLeast(trimStartMs))
                                },
                            )
                        }

                        VideoEditorMode.CROP -> {
                            CropPresetStrip(
                                selected = draftCropPreset,
                                onSelect = { selectedPreset ->
                                    draftCropPreset = selectedPreset
                                    draftCropRect = null
                                },
                                onRotate = { userRotationDegrees = (userRotationDegrees + 90) % 360 }
                            )
                            CropApplyRow(
                                selected = draftCropPreset,
                                canReset = draftCropPreset != VideoCropPreset.ORIGINAL || appliedCropPreset != VideoCropPreset.ORIGINAL,
                                onReset = {
                                    draftCropPreset = VideoCropPreset.ORIGINAL
                                    draftCropRect = null
                                    appliedCropPreset = VideoCropPreset.ORIGINAL
                                    vm.applyCrop(null)
                                    haptic()
                                },
                                onApply = {
                                    saveUndoState()
                                    appliedCropPreset = draftCropPreset
                                    val finalCrop = if (draftCropPreset == VideoCropPreset.ORIGINAL) {
                                        null
                                    } else {
                                        val nextCropRect = draftCropRect ?: defaultCropRect(draftCropPreset, currentVideoAspect)
                                        combineCrops(appliedCropRect, nextCropRect)
                                    }
                                    vm.applyCrop(finalCrop)
                                    draftCropRect = finalCrop
                                    appliedCropPreset = if (finalCrop == null) VideoCropPreset.ORIGINAL else VideoCropPreset.FREE
                                    mode = VideoEditorMode.TRIM
                                    openMode = VideoEditorMode.TRIM
                                    haptic()
                                },
                            )
                        }

                        VideoEditorMode.FILTERS -> {
                            FilterPresetStrip(
                                selected = filterPreset,
                                onSelect = { 
                                    if (filterPreset != it) {
                                        saveUndoState()
                                        filterPreset = it
                                    }
                                },
                            )
                        }

                        VideoEditorMode.AUDIO -> {
                            AudioControlRow(
                                muted = muted,
                                replacementAudioUri = replacementAudioUri,
                                videoDurationMs = trimEndMs - trimStartMs,
                                audioStartMs = draftAudioStartMs,
                                audioDurationMs = audioDurationMs,
                                onToggleMuted = {
                                    saveUndoState()
                                    muted = !muted
                                    haptic()
                                },
                                onPickAudio = {
                                    audioPicker.launch(arrayOf("audio/*"))
                                },
                            onClearAudio = {
                                saveUndoState()
                                replacementAudioUri = null
                                audioStartMs = 0L
                                draftAudioStartMs = 0L
                                previewAudioStartMs = 0L
                                audioDurationMs = 0L
                                haptic()
                            },
                            onAudioStartMsChange = {
                                draftAudioStartMs = it
                            },
                            onAudioStartMsChangeFinished = {
                                audioStartMs = draftAudioStartMs
                                previewAudioStartMs = draftAudioStartMs
                            },
                                onApply = {
                                    audioStartMs = draftAudioStartMs
                                    previewAudioStartMs = draftAudioStartMs
                                    mode = VideoEditorMode.TRIM
                                    openMode = VideoEditorMode.TRIM
                                    haptic()
                                }
                            )
                        }
                        }
                        }
                    }
                    if (!isLandscape) {
                        VideoModeDock(
                            selected = mode,
                            isMenuOpen = openMode != null,
                            onSelect = {
                                if (it == mode && openMode == it) {
                                    openMode = null
                                } else {
                                    if (it == VideoEditorMode.CROP) {
                                        draftCropPreset = VideoCropPreset.ORIGINAL
                                        draftCropRect = null
                                    }
                                    mode = it
                                    openMode = it
                                }
                                haptic()
                            },
                        )
                    }
                }
            }
        }
        
        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                VideoModeDockVertical(
                    selected = mode,
                    onSelect = {
                        if (it == mode && openMode == it) {
                            openMode = null
                        } else {
                            if (it == VideoEditorMode.CROP) {
                                draftCropPreset = VideoCropPreset.ORIGINAL
                                draftCropRect = null
                            }
                            mode = it
                            openMode = it
                        }
                        haptic()
                    },
                )
            }
        }

        if (showSaveOptions) {
            com.gorilla.gallery.ui.components.AnimatedGlassDialog(
                onDismissRequest = { showSaveOptions = false }
            ) { scale ->
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscapeLocal = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                
                com.gorilla.gallery.ui.theme.LiquidGlassSurface(
                    depth = com.gorilla.gallery.ui.theme.GlassDepth.MID, shape = RoundedCornerShape(28.dp),
                    backdrop = videoFrameBackdrop,
                    surfaceColor = appColors.bgSurface.copy(alpha = 0.92f),
                    saturationOverride = 1.55f, tintAlphaOverride = 0.07f,
                    modifier = Modifier.widthIn(min = 300.dp, max = if (isLandscapeLocal) 560.dp else 340.dp)
                        .scale(scale)
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = {}),
                ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Save Video", style = MaterialTheme.typography.titleLarge, color = appColors.textPrimary)
                            Text("Do you want to save as a new copy or overwrite the original video?", style = MaterialTheme.typography.bodyMedium, color = appColors.textSecondary, textAlign = TextAlign.Center)
                            
                            val saveAsCopy = {
                                showSaveOptions = false
                                vm.saveCopy(
                                    app = app, item = item, startMs = trimStartMs, endMs = trimEndMs, muted = muted,
                                    replacementAudioUri = replacementAudioUri, audioStartMs = audioStartMs,
                                    cropRect = if (appliedCropPreset != VideoCropPreset.ORIGINAL) appliedCropRect?.let { android.graphics.RectF(it.left, it.top, it.right, it.bottom) } else null,
                                    filterArgb = if (filterPreset != VideoFilterPreset.ORIGINAL) filterPreset.overlay.toArgb() else null,
                                    rotationDegrees = (360 - (userRotationDegrees % 360)) % 360,
                                    onDone = onClose
                                )
                            }
                            
                            val saveOverwrite = {
                                showSaveOptions = false
                                vm.overwrite(
                                    app = app, item = item, startMs = trimStartMs, endMs = trimEndMs, muted = muted,
                                    replacementAudioUri = replacementAudioUri, audioStartMs = audioStartMs,
                                    cropRect = if (appliedCropPreset != VideoCropPreset.ORIGINAL) appliedCropRect?.let { android.graphics.RectF(it.left, it.top, it.right, it.bottom) } else null,
                                    filterArgb = if (filterPreset != VideoFilterPreset.ORIGINAL) filterPreset.overlay.toArgb() else null,
                                    rotationDegrees = (360 - (userRotationDegrees % 360)) % 360,
                                    onDone = onClose
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val int1 = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                val press1 by int1.collectIsPressedAsState()
                                val scale1 by androidx.compose.animation.core.animateFloatAsState(if (press1) 0.92f else 1f, label = "s1")
                                androidx.compose.material3.Button(
                                    onClick = saveAsCopy, 
                                    interactionSource = int1,
                                    modifier = Modifier.weight(1f).graphicsLayer { scaleX = scale1; scaleY = scale1 }
                                ) {
                                    Text("Save as copy", textAlign = TextAlign.Center)
                                }

                                val int2 = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                val press2 by int2.collectIsPressedAsState()
                                val scale2 by androidx.compose.animation.core.animateFloatAsState(if (press2) 0.92f else 1f, label = "s2")
                                androidx.compose.material3.Button(
                                    onClick = saveOverwrite, 
                                    interactionSource = int2,
                                    modifier = Modifier.weight(1f).graphicsLayer { scaleX = scale2; scaleY = scale2 },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.2f), contentColor = Color.Red)
                                ) {
                                    Text("Overwrite", textAlign = TextAlign.Center)
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
private fun AppleEditorTopChrome(
    saving: Boolean,
    backdrop: Backdrop?,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit = {
        Text(
            "VIDEO",
            color = Color.White.copy(alpha = 0.52f),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
) {
    Box(modifier.widthIn(max = 520.dp).fillMaxWidth()) {
        HeaderActionButton(
            label = "Cancel",
            backdrop = backdrop,
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(96.dp),
        )

        Box(modifier = Modifier.align(Alignment.Center)) {
            centerContent()
        }

        HeaderActionButton(
            label = if (saving) "" else "Done",
            backdrop = backdrop,
            enabled = !saving,
            accentFill = true,
            onClick = onDone,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(86.dp),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalDynamicColors.current.accent,
                )
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    label: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentFill: Boolean = false,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit = {},
) {
    val accent = LocalDynamicColors.current.accent
    VideoEditorPanel(
        shape = CapsuleShape,
        backdrop = backdrop,
        enableLens = false,
        modifier = modifier
            .height(42.dp)
            .pressScale(scale = 0.88f, enabled = enabled),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CapsuleShape)
                .background(if (accentFill) accent.copy(alpha = 0.20f) else Color.Transparent)
                .border(
                    1.dp,
                    if (accentFill) accent.copy(alpha = 0.44f) else Color.White.copy(alpha = 0.10f),
                    CapsuleShape,
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (label.isNotEmpty()) {
                Text(
                    label,
                    color = if (accentFill) accent else Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
            trailingContent()
        }
    }
}

@Composable
private fun AppleTrimTimeline(
    isPlaying: Boolean,
    muted: Boolean,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    onTogglePlayback: () -> Unit,
    onToggleMuted: () -> Unit,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Row(
        modifier
            .fillMaxWidth()
            .height(if (isLandscape) 40.dp else 50.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(if (isLandscape) 40.dp else 50.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                .background(Color.White.copy(alpha = 0.40f))
                .pressScale(scale = 0.96f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTogglePlayback,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp),
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            TrimRangeControl(
                durationMs = durationMs,
                startMs = startMs,
                endMs = endMs,
                playheadMs = playheadMs,
                onStartChange = onStartChange,
                onEndChange = onEndChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        
        Box(
            Modifier
                .width(if (isLandscape) 40.dp else 50.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50))
                .background(Color.White.copy(alpha = 0.40f))
                .pressScale(scale = 0.96f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleMuted,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Mute",
                tint = Color.White,
                modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp),
            )
        }
    }
}

@Composable
private fun VideoModeDock(
    selected: VideoEditorMode,
    isMenuOpen: Boolean,
    onSelect: (VideoEditorMode) -> Unit,
) {
    val modes = VideoEditorMode.entries
    val density = LocalDensity.current
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)
    val selectedIndexState by rememberUpdatedState(selectedIndex)
    val onSelectState by rememberUpdatedState(onSelect)
    val itemSpacing = 2.dp
    val horizontalPadding = 9.dp
    var rowWidthPx by remember { mutableIntStateOf(0) }
    var dragOffsetPx by remember { mutableFloatStateOf(Float.NaN) }

    fun segmentWidthPx(): Float {
        val spacingPx = with(density) { itemSpacing.toPx() }
        return ((rowWidthPx - spacingPx * (modes.size - 1)) / modes.size).coerceAtLeast(1f)
    }

    fun segmentOffsetPx(index: Int): Float {
        val spacingPx = with(density) { itemSpacing.toPx() }
        return index * (segmentWidthPx() + spacingPx)
    }

    fun selectAt(xPx: Float) {
        if (rowWidthPx <= 0) return
        val widthPx = segmentWidthPx()
        val index = modes.indices.minBy { modeIndex ->
            abs(xPx - (segmentOffsetPx(modeIndex) + widthPx / 2f))
        }
        val maxOffsetPx = (rowWidthPx - widthPx).coerceAtLeast(0f)
        dragOffsetPx = (xPx - widthPx / 2f).coerceIn(0f, maxOffsetPx)
        if (index != selectedIndexState) {
            onSelectState(modes[index])
        }
    }

    LaunchedEffect(selectedIndex) {
        dragOffsetPx = Float.NaN
    }

    Column(Modifier.fillMaxWidth()) {
        val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        if (isMenuOpen) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
        }
        
        val bottomGlassPadding = if (isLandscape) 10.dp else 14.dp
        
        Box(
            Modifier
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = if (isMenuOpen) bottomGlassPadding else (bottomGlassPadding - 10.dp).coerceAtLeast(0.dp),
                    bottom = 0.dp
                )
                .onSizeChanged { rowWidthPx = it.width }
                .pointerInput(modes, rowWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> selectAt(offset.x) },
                        onHorizontalDrag = { change, _ ->
                            selectAt(change.position.x)
                            change.consume()
                        },
                        onDragEnd = { dragOffsetPx = Float.NaN },
                        onDragCancel = { dragOffsetPx = Float.NaN },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            var pressedMode by remember { mutableStateOf<VideoEditorMode?>(null) }
            val anyPressed = pressedMode != null
            val bgScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (anyPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 400f),
                label = "pillScale"
            )
            if (rowWidthPx > 0) {
                val selectedWidthDp = with(density) { segmentWidthPx().toDp() }
                val selectedOffsetDp by animateDpAsState(
                    targetValue = if (dragOffsetPx.isNaN()) {
                        with(density) { segmentOffsetPx(selectedIndex).toDp() }
                    } else {
                        with(density) { dragOffsetPx.toDp() }
                    },
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f),
                    label = "videoEditorModePillOffset",
                )
                val activeBgColor = accent.copy(alpha = if (appColors.isDark) 0.22f else 0.26f)

                Box(
                    Modifier
                        .offset(x = selectedOffsetDp)
                        .scale(bgScale)
                        .size(width = selectedWidthDp, height = 50.dp)
                        .clip(CapsuleShape)
                        .background(activeBgColor)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (appColors.isDark) 0.24f else 0.48f),
                            shape = CapsuleShape,
                        ),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.zIndex(1f).fillMaxWidth(),
            ) {
                modes.forEach { mode ->
                    DockModeButton(
                        mode = mode,
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.weight(1f),
                        onPressedChange = { if (it) pressedMode = mode else if (pressedMode == mode) pressedMode = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockModeButton(
    mode: VideoEditorMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPressedChange: (Boolean) -> Unit = {}
) {
    val accent = LocalDynamicColors.current.accent
    val tint = if (selected) Color.White else Color.White.copy(alpha = 0.46f)
    Column(
        modifier
            .height(50.dp)
            .clip(CapsuleShape)
            .pressScale(scale = 0.94f)
            .pointerInput(mode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressedChange(true)
                    waitForUpOrCancellation()
                    onPressedChange(false)
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            mode.icon,
            contentDescription = mode.label,
            tint = if (selected) accent else tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            mode.label,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
private fun VideoModeDockVertical(
    selected: VideoEditorMode,
    onSelect: (VideoEditorMode) -> Unit,
) {
    val modes = VideoEditorMode.entries
    val density = LocalDensity.current
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)
    val selectedIndexState by rememberUpdatedState(selectedIndex)
    val onSelectState by rememberUpdatedState(onSelect)
    val itemSpacing = 2.dp
    val verticalPadding = 9.dp
    var colHeightPx by remember { mutableIntStateOf(0) }
    var dragOffsetPx by remember { mutableFloatStateOf(Float.NaN) }

    fun segmentHeightPx(): Float {
        val spacingPx = with(density) { itemSpacing.toPx() }
        return ((colHeightPx - spacingPx * (modes.size - 1)) / modes.size).coerceAtLeast(1f)
    }

    fun segmentOffsetPx(index: Int): Float {
        val spacingPx = with(density) { itemSpacing.toPx() }
        return index * (segmentHeightPx() + spacingPx)
    }

    fun selectAt(yPx: Float) {
        if (colHeightPx <= 0) return
        val heightPx = segmentHeightPx()
        val index = modes.indices.minBy { modeIndex ->
            abs(yPx - (segmentOffsetPx(modeIndex) + heightPx / 2f))
        }
        val maxOffsetPx = (colHeightPx - heightPx).coerceAtLeast(0f)
        dragOffsetPx = (yPx - heightPx / 2f).coerceIn(0f, maxOffsetPx)
        if (index != selectedIndexState) {
            onSelectState(modes[index])
        }
    }

    LaunchedEffect(selectedIndex) {
        dragOffsetPx = Float.NaN
    }

    Column(Modifier.fillMaxHeight()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
        Box(
            Modifier
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .onSizeChanged { colHeightPx = it.height }
                .pointerInput(modes, colHeightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> selectAt(offset.y) },
                        onVerticalDrag = { change, _ ->
                            selectAt(change.position.y)
                            change.consume()
                        },
                        onDragEnd = { dragOffsetPx = Float.NaN },
                        onDragCancel = { dragOffsetPx = Float.NaN },
                    )
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            var pressedMode by remember { mutableStateOf<VideoEditorMode?>(null) }
            val anyPressed = pressedMode != null
            val bgScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (anyPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 400f),
                label = "pillScaleVertical"
            )
            if (colHeightPx > 0) {
                val selectedHeightDp = with(density) { segmentHeightPx().toDp() }
                val selectedOffsetDp by animateDpAsState(
                    targetValue = if (dragOffsetPx.isNaN()) {
                        with(density) { segmentOffsetPx(selectedIndex).toDp() }
                    } else {
                        with(density) { dragOffsetPx.toDp() }
                    },
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f),
                    label = "videoEditorModePillOffsetVertical",
                )
                val activeBgColor = accent.copy(alpha = if (appColors.isDark) 0.22f else 0.26f)

                Box(
                    Modifier
                        .offset(y = selectedOffsetDp)
                        .scale(bgScale)
                        .size(width = 56.dp, height = selectedHeightDp)
                        .clip(CapsuleShape)
                        .background(activeBgColor)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (appColors.isDark) 0.24f else 0.48f),
                            shape = CapsuleShape,
                        ),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.zIndex(1f),
            ) {
                modes.forEach { mode ->
                    DockModeButtonVertical(
                        mode = mode,
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.height(if (colHeightPx > 0) with(density) { segmentHeightPx().toDp() } else 58.dp),
                        onPressedChange = { if (it) pressedMode = mode else if (pressedMode == mode) pressedMode = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockModeButtonVertical(
    mode: VideoEditorMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPressedChange: (Boolean) -> Unit = {}
) {
    val accent = LocalDynamicColors.current.accent
    val tint = if (selected) Color.White else Color.White.copy(alpha = 0.46f)
    Column(
        modifier
            .width(56.dp)
            .clip(CapsuleShape)
            .pressScale(scale = 0.94f)
            .pointerInput(mode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressedChange(true)
                    waitForUpOrCancellation()
                    onPressedChange(false)
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            mode.icon,
            contentDescription = mode.label,
            tint = if (selected) accent else tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            mode.label,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 8.sp,
        )
    }
}

@Composable
private fun CropPresetStrip(
    selected: VideoCropPreset,
    onSelect: (VideoCropPreset) -> Unit,
    onRotate: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .softHorizontalScrollEdges(scrollState)
            .horizontalScroll(scrollState)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeaturePill(
            icon = { tint ->
                Icon(
                    Icons.Rounded.Rotate90DegreesCw,
                    contentDescription = "Rotate",
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = "Rotate",
            selected = false,
            onClick = onRotate,
        )
        VideoCropPreset.entries.filter { it != VideoCropPreset.ORIGINAL }.forEach { preset ->
            FeaturePill(
                icon = { tint ->
                    Icon(
                        Icons.Rounded.Crop,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp),
                    )
                },
                label = preset.label,
                selected = preset == selected,
            ) {
                onSelect(preset)
            }
        }
    }
}

@Composable
private fun CropApplyRow(
    selected: VideoCropPreset,
    canReset: Boolean,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeaturePill(
            icon = { tint ->
                Icon(
                    Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = "Reset",
            selected = false,
            enabled = canReset,
            modifier = Modifier.weight(1f),
            onClick = onReset,
        )
        FeaturePill(
            icon = { tint ->
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = if (selected == VideoCropPreset.ORIGINAL) "Apply Original" else "Apply ${selected.label}",
            selected = true,
            modifier = Modifier.weight(1f),
            onClick = onApply,
        )
    }
}

@Composable
private fun FilterPresetStrip(
    selected: VideoFilterPreset,
    onSelect: (VideoFilterPreset) -> Unit,
) {
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    Row(
        Modifier
            .then(if (isLandscape) Modifier else Modifier.fillMaxWidth())
            .softHorizontalScrollEdges(scrollState)
            .horizontalScroll(scrollState)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        VideoFilterPreset.entries.forEach { preset ->
            FeaturePill(
                icon = { tint ->
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (preset == VideoFilterPreset.ORIGINAL) {
                                    LocalAppColors.current.textSecondary.copy(alpha = 0.30f)
                                } else {
                                    preset.overlay.copy(alpha = 0.70f)
                                },
                            )
                            .border(
                                1.dp,
                                tint,
                                CircleShape,
                            ),
                    )
                },
                label = preset.label,
                selected = preset == selected,
            ) {
                onSelect(preset)
            }
        }
    }
}

private fun Modifier.softHorizontalScrollEdges(
    scrollState: ScrollState,
    fadeWidth: Dp = 48.dp,
): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()

        val fadePx = fadeWidth.toPx().coerceAtMost(size.width / 2f)
        if (fadePx <= 0f) return@drawWithContent

        if (scrollState.value > 0) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        1f to Color.Black,
                    ),
                    startX = 0f,
                    endX = fadePx,
                ),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn,
            )
        }

        if (scrollState.value < scrollState.maxValue) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    startX = size.width - fadePx,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
    }

@Composable
private fun AudioControlRow(
    muted: Boolean,
    replacementAudioUri: android.net.Uri?,
    videoDurationMs: Long,
    audioStartMs: Long,
    audioDurationMs: Long,
    onToggleMuted: () -> Unit,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onAudioStartMsChange: (Long) -> Unit,
    onAudioStartMsChangeFinished: () -> Unit,
    onApply: () -> Unit,
) {
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Column(modifier = if (isLandscape) Modifier else Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = if (isLandscape) Modifier else Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            FeaturePill(
                icon = { tint ->
                    Icon(
                        if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(17.dp),
                    )
                },
                label = if (muted) "Muted" else "Original audio",
                selected = muted,
                modifier = if (isLandscape) Modifier else Modifier.weight(1f),
                onClick = onToggleMuted,
            )

            FeaturePill(
                icon = { tint ->
                    Icon(
                        Icons.Rounded.Audiotrack,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(17.dp),
                    )
                },
                label = if (replacementAudioUri != null) "Audio replaced" else "Replace audio",
                selected = replacementAudioUri != null,
                modifier = if (isLandscape) Modifier else Modifier.weight(1f),
                onClick = {
                    if (replacementAudioUri != null) {
                        onClearAudio()
                    } else {
                        onPickAudio()
                    }
                },
            )
        }

        if (replacementAudioUri != null && audioDurationMs > 0) {
            Spacer(Modifier.height(16.dp))
            val audioEndMs = (audioStartMs + videoDurationMs).coerceAtMost(audioDurationMs)
            Text(
                "Audio portion: ${formatDuration(audioStartMs)} - ${formatDuration(audioEndMs)}", 
                style = MaterialTheme.typography.bodySmall, 
                color = LocalAppColors.current.textSecondary, 
                modifier = Modifier.padding(start = 12.dp)
            )
            androidx.compose.material3.Slider(
                value = audioStartMs.toFloat(),
                onValueChange = { onAudioStartMsChange(it.toLong()) },
                onValueChangeFinished = onAudioStartMsChangeFinished,
                valueRange = 0f..audioDurationMs.toFloat(),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = LocalDynamicColors.current.accent,
                    activeTrackColor = LocalDynamicColors.current.accent,
                )
            )
            Spacer(Modifier.height(8.dp))
            FeaturePill(
                icon = { tint ->
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp),
                    )
                },
                label = "Apply Audio",
                selected = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                onClick = onApply,
            )
        }
    }
}

@Composable
private fun VideoPlayerView(
    player: ExoPlayer,
    resizeMode: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            (android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.video_player_texture, null) as PlayerView).apply {
                this.player = player
                useController = false
                this.resizeMode = resizeMode
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { view ->
            view.player = player
            view.resizeMode = resizeMode
        },
        onRelease = { view ->
            view.player = null
        },
        modifier = modifier,
    )
}


@Composable
private fun BoxScope.VideoFilterOverlay(filter: VideoFilterPreset) {
    if (filter == VideoFilterPreset.ORIGINAL) return
    Box(
        Modifier
            .matchParentSize()
            .background(filter.overlay),
    )
}

@OptIn(UnstableApi::class)
private fun buildPreviewMediaSource(
    context: android.content.Context,
    videoUri: android.net.Uri,
    replacementAudioUri: android.net.Uri?,
    audioStartMs: Long,
): MediaSource {
    val mediaSourceFactory = DefaultMediaSourceFactory(context)
    val videoSource = mediaSourceFactory.createMediaSource(ExoMediaItem.fromUri(videoUri))
    if (replacementAudioUri == null) {
        return videoSource
    }

    val videoOnlySource = FilteringMediaSource(videoSource, C.TRACK_TYPE_VIDEO)
    val audioItem = ExoMediaItem.Builder()
        .setUri(replacementAudioUri)
        .setClippingConfiguration(
            ExoMediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(audioStartMs)
                .build()
        )
        .build()
    val audioOnlySource = FilteringMediaSource(mediaSourceFactory.createMediaSource(audioItem), C.TRACK_TYPE_AUDIO)
    return MergingMediaSource(
        true,
        false,
        videoOnlySource,
        audioOnlySource,
    )
}

private fun readMediaDurationMs(context: android.content.Context, uri: android.net.Uri): Long {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            retriever.setDataSource(pfd.fileDescriptor)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
        } ?: 0L
    } catch (_: Exception) {
        0L
    } finally {
        retriever.release()
    }
}

@Composable
private fun BoxScope.CropSelectionOverlay(
    preset: VideoCropPreset,
    cropRect: Rect?,
    onCropRectChange: (Rect) -> Unit,
) {
    BoxWithConstraints(Modifier.matchParentSize()) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value
        if (widthDp <= 0f || heightDp <= 0f) return@BoxWithConstraints

        val normalizedRect = cropRect ?: defaultCropRect(preset, widthDp / heightDp.coerceAtLeast(1f))
        val overlayRect = normalizedRect.toDpRect(widthDp, heightDp)

        LaunchedEffect(preset, widthDp, heightDp) {
            if (cropRect == null) {
                onCropRectChange(normalizedRect)
            } else if (preset != VideoCropPreset.FREE && preset != VideoCropPreset.ORIGINAL) {
                val snapped = snapToAspect(
                    current = overlayRect,
                    aspect = preset.cropAspect,
                    imageWidth = widthDp,
                    imageHeight = heightDp,
                )
                onCropRectChange(snapped.toNormalizedRect(widthDp, heightDp))
            }
        }

        CropOverlay(
            cropAspect = preset.cropAspect,
            cropRect = overlayRect,
            onCropRectChange = { onCropRectChange(it.toNormalizedRect(widthDp, heightDp)) },
            imageWidth = widthDp,
            imageHeight = heightDp,
        )
    }
}

private fun defaultCropRect(preset: VideoCropPreset, containerRatio: Float): Rect {
    if (preset == VideoCropPreset.ORIGINAL) return Rect(0f, 0f, 1f, 1f)
    val desiredRatio = preset.ratio
    val width: Float
    val height: Float
    if (desiredRatio == null) {
        width = 0.76f
        height = 0.54f
    } else if (containerRatio > desiredRatio) {
        height = 0.78f
        width = (height * desiredRatio / containerRatio).coerceAtMost(0.88f)
    } else {
        width = 0.78f
        height = (width * containerRatio / desiredRatio).coerceAtMost(0.88f)
    }
    val left = (1f - width) / 2f
    val top = (1f - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun Rect.toDpRect(widthDp: Float, heightDp: Float): Rect {
    return Rect(
        left = left * widthDp,
        top = top * heightDp,
        right = right * widthDp,
        bottom = bottom * heightDp,
    )
}

private fun Rect.toNormalizedRect(widthDp: Float, heightDp: Float): Rect {
    return Rect(
        left = (left / widthDp.coerceAtLeast(1f)).coerceIn(0f, 1f),
        top = (top / heightDp.coerceAtLeast(1f)).coerceIn(0f, 1f),
        right = (right / widthDp.coerceAtLeast(1f)).coerceIn(0f, 1f),
        bottom = (bottom / heightDp.coerceAtLeast(1f)).coerceIn(0f, 1f),
    )
}

@Composable
private fun VideoEditorPanel(
    shape: RoundedCornerShape,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enableLens: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.kernelSuGlassBackdrop(
            shape = shape,
            backdrop = backdrop,
            enableLens = enableLens
        ),
    ) {
        content()
    }
}

@Composable
private fun FeaturePill(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val primaryText = appColors.textPrimary
    val secondaryText = appColors.textSecondary
    
    val bgColor by androidx.compose.animation.animateColorAsState(
        if (selected) accent.copy(alpha = 0.20f) else appColors.bgGlass,
        label = "pillBgColor"
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        if (selected) accent.copy(alpha = 0.42f) else appColors.borderGlass,
        label = "pillBorderColor"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        if (selected) primaryText else secondaryText,
        label = "pillContentColor"
    )

    Row(
        modifier
            .height(32.dp)
            .clip(CapsuleShape)
            .then(if (onClick != null && enabled) Modifier.pressScale(scale = 0.94f) else Modifier)
            .background(bgColor)
            .border(1.dp, borderColor, CapsuleShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                waitForUpOrCancellation()
                            }
                        }
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(if (selected) accent else contentColor)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TrimRangeControl(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalDynamicColors.current.accent
    val latestStartFraction by rememberUpdatedState(startMs.toFloat() / durationMs.toFloat())
    val latestEndFraction by rememberUpdatedState(endMs.toFloat() / durationMs.toFloat())
    val latestOnStartChange by rememberUpdatedState(onStartChange)
    val latestOnEndChange by rememberUpdatedState(onEndChange)
    BoxWithConstraints(modifier.height(54.dp)) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val startFraction = startMs.toFloat() / durationMs.toFloat()
        val endFraction = endMs.toFloat() / durationMs.toFloat()
        val playheadFraction = playheadMs.toFloat() / durationMs.toFloat()

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(durationMs, widthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val downFraction = (down.position.x / widthPx).coerceIn(0f, 1f)
                        val grabStart = abs(downFraction - latestStartFraction) <= abs(downFraction - latestEndFraction)
                        fun update(x: Float) {
                            val ms = ((x / widthPx).coerceIn(0f, 1f) * durationMs).toLong()
                            if (grabStart) latestOnStartChange(ms) else latestOnEndChange(ms)
                        }
                        update(down.position.x)
                        drag(down.id) { change ->
                            change.consume()
                            update(change.position.x)
                        }
                    }
                },
        ) {
            val trackTop = 6.dp.toPx()
            val trackHeight = size.height - 12.dp.toPx()
            val radius = 6.dp.toPx()
            val startX = startFraction * size.width
            val endX = endFraction * size.width
            val playheadX = playheadFraction.coerceIn(0f, 1f) * size.width
            val frameGap = 2.dp.toPx()
            val frameCount = 11
            val frameWidth = (size.width - frameGap * (frameCount + 1)) / frameCount

            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(radius),
            )

            repeat(frameCount) { index ->
                val left = frameGap + index * (frameWidth + frameGap)
                val alpha = if (index % 3 == 0) 0.32f else 0.22f
                drawRoundRect(
                    color = Color.White.copy(alpha = alpha),
                    topLeft = Offset(left, trackTop + frameGap),
                    size = Size(frameWidth, trackHeight - frameGap * 2),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.34f),
                topLeft = Offset(0f, trackTop),
                size = Size(startX.coerceAtLeast(0f), trackHeight),
                cornerRadius = CornerRadius(radius),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.34f),
                topLeft = Offset(endX, trackTop),
                size = Size((size.width - endX).coerceAtLeast(0f), trackHeight),
                cornerRadius = CornerRadius(radius),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(startX - 3.dp.toPx(), trackTop - 5.dp.toPx()),
                size = Size(6.dp.toPx(), trackHeight + 10.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(endX - 3.dp.toPx(), trackTop - 5.dp.toPx()),
                size = Size(6.dp.toPx(), trackHeight + 10.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.86f),
                topLeft = Offset(playheadX - 1.dp.toPx(), 5.dp.toPx()),
                size = Size(2.dp.toPx(), size.height - 10.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
    }
}
