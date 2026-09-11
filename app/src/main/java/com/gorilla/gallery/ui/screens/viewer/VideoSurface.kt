package com.gorilla.gallery.ui.screens.viewer
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.components.formatDuration
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic
import com.gorilla.gallery.ui.theme.CapsuleShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.gorilla.gallery.ui.theme.kernelSuGlassBackdrop

/** Inline, lifecycle-scoped ExoPlayer with a Compose glass control panel. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoSurface(
    item: MediaItem,
    onClose: () -> Unit,
    onMenu: () -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onEdit: () -> Unit,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onHideControls: () -> Unit,
    isActive: Boolean,
    dismissRequested: Boolean,
    cropToFill: Boolean,
    isDismissingMedia: Boolean = false,
    previewBitmap: android.graphics.Bitmap? = null,
    highQualityThumbnails: Boolean = false,
    onProvideBackdrop: (com.kyant.backdrop.Backdrop) -> Unit = {},
    modifier: Modifier = Modifier,
    videoModifier: Modifier = Modifier,
    zoomModifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = LocalDynamicColors.current.accent
    val chromeIconTint = viewerChromeContentColor()
    val chromeSecondaryTint = viewerChromeSecondaryColor()
    val haptic = rememberHaptic()
    val videoFrameBackdrop = rememberLayerBackdrop()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val loopVideos = com.gorilla.gallery.ui.theme.LocalAppSettings.current.loopVideos

    LaunchedEffect(videoFrameBackdrop) {
        onProvideBackdrop(videoFrameBackdrop)
    }

    val exo = remember(item.uri) {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            repeatMode = if (loopVideos) androidx.media3.common.Player.REPEAT_MODE_ONE else androidx.media3.common.Player.REPEAT_MODE_OFF
            prepare()
        }
    }

    LaunchedEffect(loopVideos, exo) {
        exo.repeatMode = if (loopVideos) androidx.media3.common.Player.REPEAT_MODE_ONE else androidx.media3.common.Player.REPEAT_MODE_OFF
    }

    var isPlaying by remember { mutableStateOf(true) }
    var inPip by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }
    
    val enablePipMode = com.gorilla.gallery.ui.theme.LocalAppSettings.current.enablePipMode
    val activity = context as? androidx.activity.ComponentActivity
    
    DisposableEffect(activity) {
        val listener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            inPip = info.isInPictureInPictureMode
        }
        activity?.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(listener)
        }
    }

    LaunchedEffect(enablePipMode, isPlaying, isActive, videoWidth, videoHeight, activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && activity != null) {
            if (isActive) {
                try {
                    val builder = android.app.PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(enablePipMode && isPlaying)
                    
                    if (videoWidth > 0 && videoHeight > 0) {
                        val ratio = videoWidth.toFloat() / videoHeight.toFloat()
                        val clampedRatio = ratio.coerceIn(18f / 43f, 43f / 18f)
                        val w = (clampedRatio * 10000).toInt()
                        val h = 10000
                        builder.setAspectRatio(android.util.Rational(w, h))
                    }
                    
                    activity.setPictureInPictureParams(builder.build())
                } catch (e: Exception) {}
            }
        }
    }
    DisposableEffect(activity) {
        onDispose {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && activity != null) {
                try {
                    val params = android.app.PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
                    activity.setPictureInPictureParams(params)
                } catch (e: Exception) {}
            }
        }
    }

    var muted by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var wasPlayingBeforeScrub by remember { mutableStateOf(false) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.unappliedRotationDegrees % 180 != 0) {
                    videoWidth = videoSize.height
                    videoHeight = videoSize.width
                } else {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    exo.seekTo(0)
                    exo.pause()
                }
            }
        }
        exo.addListener(listener)
        // clearVideoSurface() before release pushes a blank frame to the TextureView, so it can't
        // leave its last decoded frame lingering in the window after the view detaches — that
        // leftover frame was the video "ghost" briefly visible over the grid after swiping out.
        onDispose { exo.removeListener(listener); exo.clearVideoSurface(); exo.release() }
    }

    // Pause when the app is backgrounded.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) exo.pause() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(isActive, dismissRequested) {
        val shouldPlay = isActive && !dismissRequested
        exo.playWhenReady = shouldPlay
        if (shouldPlay) {
            exo.play()
        } else {
            exo.pause()
            exo.seekTo(maxOf(0L, exo.currentPosition))
        }
    }

    // Poll position for the seek bar.
    LaunchedEffect(exo) {
        while (true) {
            positionMs = exo.currentPosition
            if (exo.duration > 0) durationMs = exo.duration
            kotlinx.coroutines.delay(200)
        }
    }

    // Keep the main composition invalidating while playing so layerBackdrop updates
    // when the bottom sheets (in separate Dialog windows) are open and controls are hidden.
    var frameTrigger by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(isPlaying, isActive) {
        while (isPlaying && isActive) {
            androidx.compose.runtime.withFrameNanos { }
            frameTrigger++
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    // Read state to invalidate the graphics layer every frame
                    val trigger = frameTrigger
                    alpha = if (trigger >= 0) 1f else 0.99f
                }
                .layerBackdrop(videoFrameBackdrop), 
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.fillMaxSize().then(zoomModifier), contentAlignment = Alignment.Center) {
                Box(modifier = videoModifier) {
                    AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
    
                if (!isDismissingMedia) {
                    AndroidView(
                        // Inflated from XML so the PlayerView uses a TextureView surface — a SurfaceView ignores
                        // the Compose transforms applied during the swipe-dismiss, leaving the video "stuck" at
                        // full size over the revealed grid. A TextureView animates with the rest of the viewer.
                        factory = { ctx ->
                            (android.view.LayoutInflater.from(ctx)
                                .inflate(com.gorilla.gallery.R.layout.video_player_texture, null) as PlayerView).apply {
                                player = exo
                                resizeMode = if (cropToFill) {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                } else {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                layoutParams = android.widget.FrameLayout.LayoutParams(
                                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                isClickable = false
                                isFocusable = false
                                setOnTouchListener { _, _ -> false }
                            }
                        },
                        update = { view ->
                            view.resizeMode = if (cropToFill) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        onRelease = { view ->
                            view.player = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (previewBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            }
        }
        
        // Interactive layer
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top HUD
            if (isLandscape) {
                AnimatedVisibility(
                    visible = controlsVisible && !inPip,
                    enter = slideInVertically(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) { -it } + fadeIn(animationSpec = tween(durationMillis = 100, easing = LinearEasing)),
                    exit = slideOutVertically(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) { -it } + fadeOut(animationSpec = tween(durationMillis = 100, easing = LinearEasing)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        val (day, time) = remember(item.dateTakenMs) { com.gorilla.gallery.ui.screens.viewer.viewerDateLabels(item.dateTakenMs) }
                        Box(
                            modifier = Modifier
                                .height(52.dp)
                                .wrapContentWidth()
                                .clip(CapsuleShape)
                                .pressScale(scale = 0.94f)
                                .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) }
                                .kernelSuGlassBackdrop(
                                    backdrop = videoFrameBackdrop,
                                    shape = CapsuleShape
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
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = chromeIconTint,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Column {
                                    if (day != null) {
                                        Text(
                                            text = day,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = chromeIconTint,
                                        )
                                    }
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                            fontSize = 10.5.sp
                                        ),
                                        color = chromeSecondaryTint,
                                    )
                                }
                            }
                        }

                        val showActionPill = item.trashId == null && item.secureId == null
                        if (showActionPill) {
                            Box(
                                modifier = Modifier
                                    .height(52.dp)
                                    .kernelSuGlassBackdrop(
                                        backdrop = videoFrameBackdrop,
                                        shape = CapsuleShape
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VideoActionIcon(onClick = onFavorite) {
                                        Icon(
                                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFavorite) accent else chromeIconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    VideoActionIcon(onClick = onEdit) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = chromeIconTint, modifier = Modifier.size(20.dp))
                                    }
                                    VideoActionIcon(onClick = onShare) {
                                        Icon(Icons.Rounded.Share, contentDescription = "Share", tint = chromeIconTint, modifier = Modifier.size(20.dp))
                                    }
                                    VideoActionIcon(onClick = onDelete) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = chromeIconTint, modifier = Modifier.size(20.dp))
                                    }
                                    VideoActionIcon(onClick = onInfo) {
                                        Icon(Icons.Rounded.Info, contentDescription = "Info", tint = chromeIconTint, modifier = Modifier.size(20.dp))
                                    }
                                    VideoActionIcon(onClick = onMenu) {
                                        Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = chromeIconTint, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible && !inPip,
                enter = slideInVertically(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(durationMillis = 100, easing = LinearEasing)),
                exit = slideOutVertically(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) { it } + fadeOut(animationSpec = tween(durationMillis = 100, easing = LinearEasing)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                if (isLandscape) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 740.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(52.dp)
                                .kernelSuGlassBackdrop(
                                    backdrop = videoFrameBackdrop,
                                    shape = CapsuleShape
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(start = 10.dp, end = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dur = durationMs.coerceAtLeast(1)
                                val displayPositionMs = if (isScrubbing) scrubPositionMs else positionMs

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .pressScale(scale = 0.88f)
                                            .clip(CircleShape)
                                            .pointerInput(Unit) {
                                                detectTapGestures(onTap = {
                                                    haptic()
                                                    val newPos = maxOf(0L, positionMs - 5000L)
                                                    exo.seekTo(newPos)
                                                    positionMs = newPos
                                                })
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Replay5,
                                            contentDescription = "Rewind 5s",
                                            tint = chromeSecondaryTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .pressScale(scale = 0.88f)
                                            .clip(CircleShape)
                                            .background(accent.copy(alpha = 0.16f))
                                            .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
                                            .pointerInput(Unit) {
                                                detectTapGestures(onTap = {
                                                    haptic()
                                                    if (exo.isPlaying) exo.pause() else exo.play()
                                                })
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .pressScale(scale = 0.88f)
                                            .clip(CircleShape)
                                            .pointerInput(Unit) {
                                                detectTapGestures(onTap = {
                                                    haptic()
                                                    val newPos = minOf(dur, positionMs + 5000L)
                                                    exo.seekTo(newPos)
                                                    positionMs = newPos
                                                })
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Forward5,
                                            contentDescription = "Forward 5s",
                                            tint = chromeSecondaryTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(2.dp))

                                Text(
                                    text = formatDuration(displayPositionMs),
                                    color = chromeSecondaryTint,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.widthIn(min = 28.dp).offset(y = 1.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                VideoSeekBar(
                                    positionMs = displayPositionMs,
                                    position = (displayPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f),
                                    onSeek = { frac ->
                                        if (!isScrubbing) {
                                            wasPlayingBeforeScrub = exo.isPlaying
                                            exo.pause()
                                            isScrubbing = true
                                        }
                                        scrubPositionMs = (frac * dur).toLong()
                                        exo.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                                        exo.seekTo(scrubPositionMs)
                                    },
                                    onSeekFinished = { frac ->
                                        val targetPos = (frac * dur).toLong()
                                        exo.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
                                        exo.seekTo(targetPos)
                                        positionMs = targetPos
                                        isScrubbing = false
                                        if (wasPlayingBeforeScrub) exo.play()
                                    },
                                    modifier = Modifier.weight(1f),
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = formatDuration(durationMs),
                                    color = chromeSecondaryTint,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.widthIn(min = 28.dp).offset(y = 1.dp)
                                )

                                Spacer(modifier = Modifier.width(1.dp))

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .pressScale(scale = 0.88f)
                                        .clip(CircleShape)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = {
                                                muted = !muted
                                                exo.volume = if (muted) 0f else 1f
                                            })
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "Mute",
                                        tint = chromeSecondaryTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .kernelSuGlassBackdrop(
                                    backdrop = videoFrameBackdrop,
                                    shape = RoundedCornerShape(percent = 50)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 10.dp, start = 12.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dur = durationMs.coerceAtLeast(1)
                                val displayPositionMs = if (isScrubbing) scrubPositionMs else positionMs

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pressScale(scale = 0.88f)
                                        .clip(CircleShape)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = {
                                                haptic()
                                                if (exo.isPlaying) exo.pause() else exo.play()
                                            })
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = chromeIconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = formatDuration(displayPositionMs),
                                    color = chromeSecondaryTint,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.width(34.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                VideoSeekBar(
                                    positionMs = displayPositionMs,
                                    position = (displayPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f),
                                    onSeek = { frac ->
                                        if (!isScrubbing) {
                                            wasPlayingBeforeScrub = exo.isPlaying
                                            exo.pause()
                                            isScrubbing = true
                                        }
                                        scrubPositionMs = (frac * dur).toLong()
                                        exo.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                                        exo.seekTo(scrubPositionMs)
                                    },
                                    onSeekFinished = { frac ->
                                        val targetPos = (frac * dur).toLong()
                                        exo.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
                                        exo.seekTo(targetPos)
                                        positionMs = targetPos
                                        isScrubbing = false
                                        if (wasPlayingBeforeScrub) exo.play()
                                    },
                                    modifier = Modifier.weight(1f),
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = formatDuration(durationMs),
                                    color = chromeSecondaryTint,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.width(34.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pressScale(scale = 0.88f)
                                        .clip(CircleShape)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = {
                                                muted = !muted
                                                exo.volume = if (muted) 0f else 1f
                                            })
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "Mute",
                                        tint = chromeIconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        val showActionPill = item.trashId == null && item.secureId == null
                        Box(
                            modifier = Modifier
                                .alpha(if (showActionPill) 1f else 0f)
                                .kernelSuGlassBackdrop(
                                    backdrop = videoFrameBackdrop,
                                    shape = CapsuleShape
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VideoActionIconOld(onClick = if (showActionPill) onFavorite else { {} }) {
                                    Icon(
                                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) accent else chromeIconTint,
                                    )
                                }
                                VideoActionIconOld(onClick = if (showActionPill) onEdit else { {} }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = chromeIconTint)
                                }
                                VideoActionIconOld(onClick = if (showActionPill) onShare else { {} }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share", tint = chromeIconTint)
                                }
                                VideoActionIconOld(onClick = if (showActionPill) onDelete else { {} }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = chromeIconTint)
                                }
                                VideoActionIconOld(onClick = if (showActionPill) onInfo else { {} }) {
                                    Icon(Icons.Rounded.Info, contentDescription = "Info", tint = chromeIconTint)
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
private fun VideoActionIcon(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .pressScale(scale = 0.88f)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun VideoActionIconOld(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.pressScale(scale = 0.88f),
        content = content
    )
}

@Composable
private fun videoLiquidSurfaceColor(): Color {
    return viewerChromeSurfaceColor()
}

@Composable
private fun videoLiquidTintAlpha(): Float? {
    return viewerChromeTintAlpha()
}

@Composable
private fun videoLiquidSaturation(): Float? {
    return viewerChromeSaturation()
}

/**
 * Fully custom, continuously-draggable scrubber. [position] is a 0f..1f fraction; [onSeek]
 * fires on the initial touch and on every drag move. [onSeekFinished] fires on touch release
 * to seek the player.
 */

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VideoSeekBar(
    positionMs: Long,
    position: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = LocalDynamicColors.current.accent
    val sliderStyle = com.gorilla.gallery.ui.theme.LocalPlayerSliderStyle.current
    val sliderColors = androidx.compose.material3.SliderDefaults.colors(
        activeTrackColor = accentColor,
        inactiveTrackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.24f),
        thumbColor = accentColor
    )
    val fixedModifier = modifier.height(36.dp)
    val customSliderModifier = fixedModifier.padding(horizontal = 10.dp)
    
    var lastValue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(position) }
    val handleSeek: (Float) -> Unit = { 
        lastValue = it
        onSeek(it)
    }

    when (sliderStyle) {
        com.gorilla.gallery.data.settings.PlayerSliderStyle.DEFAULT -> {
            androidx.compose.material3.Slider(
                value = position,
                onValueChange = handleSeek,
                onValueChangeFinished = { onSeekFinished(lastValue) },
                colors = sliderColors,
                modifier = fixedModifier,
                track = { sliderState ->
                    // Grow the gap from 0 to 6dp over the first second so the tail is visible immediately
                    val gap = (positionMs.toFloat() / 1000f * 6f).coerceIn(0f, 6f).dp
                    androidx.compose.material3.SliderDefaults.Track(
                        sliderState = sliderState,
                        colors = sliderColors,
                        thumbTrackGapSize = gap
                    )
                }
            )
        }

        com.gorilla.gallery.data.settings.PlayerSliderStyle.SLIM -> {
            val trackInteractionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
            val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
            val isTrackActive = isTrackDragged || isTrackPressed

            val trackHeight by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isTrackActive) 16.dp else 10.dp,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "trackHeight"
            )

            androidx.compose.material3.Slider(
                value = position,
                onValueChange = handleSeek,
                onValueChangeFinished = { onSeekFinished(lastValue) },
                colors = sliderColors,
                interactionSource = trackInteractionSource,
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState -> 
                    com.gorilla.gallery.ui.components.sliders.PlayerSliderTrack(
                        sliderState = sliderState, 
                        colors = sliderColors,
                        trackHeight = trackHeight
                    ) 
                },
                modifier = customSliderModifier
            )
        }
        com.gorilla.gallery.data.settings.PlayerSliderStyle.SQUIGGLY -> {
            com.gorilla.gallery.ui.components.sliders.SquigglySlider(value = position, onValueChange = handleSeek, onValueChangeFinished = { onSeekFinished(it) }, colors = sliderColors, modifier = customSliderModifier)
        }
    }
}
