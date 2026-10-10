package com.gorilla.gallery.ui.screens.editor
import androidx.compose.foundation.basicMarquee

import kotlinx.coroutines.launch
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.defaultMinSize

import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures


import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.ui.draw.alpha

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import me.saket.telephoto.zoomable.zoomable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.text.style.TextAlign
import com.gorilla.gallery.ui.components.GlassAlertDialog
import com.gorilla.gallery.ui.components.GlassCustomDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Brush as GraphicsBrush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gorilla.gallery.data.model.CropAspect
import com.gorilla.gallery.data.model.EditMatrix
import com.gorilla.gallery.data.model.EditState
import com.gorilla.gallery.data.model.FilterPreset
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.GlassSlider
import com.gorilla.gallery.ui.theme.CapsuleShape
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.kernelSuGlassBackdrop
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic
import androidx.compose.ui.graphics.asImageBitmap
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

private enum class Tool(val label: String) {
    CROP("Crop"), ROTATE("Rotate"), DOODLE("Doodle"), BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"), SATURATION("Saturation"), WARMTH("Warmth")
}

private enum class PhotoEditorMode(val label: String, val icon: ImageVector) {
    EDIT("Edit", Icons.Rounded.Tune),
    DOODLE("Doodle", Icons.Rounded.Brush),
    FILTERS("Filters", Icons.Rounded.FilterVintage),
    CROP("Crop", Icons.Rounded.Crop),
}

private enum class EditOption(val label: String) {
    ROTATE("Rotate"),
    EXPOSURE("Exposure"),
    BRILLIANCE("Brilliance"),
    BRIGHTNESS("Brightness"),
    HIGHLIGHTS("Highlights"),
    SHADOWS("Shadows"),
    CONTRAST("Contrast"),
    SATURATION("Saturation"),
    VIBRANCY("Vibrancy"),
    WARMTH("Warmth"),
    VIGNETTE("Vignette"),
    SHARPNESS("Sharpness"),
}

@Composable
fun EditorScreen(
    app: AppViewModel,
    item: MediaItem,
    onClose: () -> Unit,
    vm: EditorViewModel = viewModel(factory = EditorViewModel.Factory),
) {
    var edit by remember { mutableStateOf(EditState()) }
    var tool by remember { mutableStateOf<Tool?>(null) }
    var mode by remember { mutableStateOf(PhotoEditorMode.EDIT) }
    var openMode by remember { mutableStateOf<PhotoEditorMode?>(PhotoEditorMode.EDIT) }
    var editOption by remember { mutableStateOf(EditOption.ROTATE) }
    var previousCropState by remember { mutableStateOf<EditState?>(null) }
    val editUndoStack = remember { ArrayDeque<EditState>() }
    val editRedoStack = remember { ArrayDeque<EditState>() }
    var confirmOverwrite by remember { mutableStateOf(false) }
    var showSaveOptions by remember { mutableStateOf(false) }
    var showOriginalPreview by remember { mutableStateOf(false) }
    val saving by vm.saving.collectAsStateWithLifecycle()
    val accent = LocalDynamicColors.current.accent
    val workingBmp = vm.workingBitmap.value
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    fun setEditState(next: EditState) {
        if (next == edit) return
        editUndoStack.addLast(edit)
        if (editUndoStack.size > 50) editUndoStack.removeFirst()
        editRedoStack.clear()
        edit = next
    }

    fun isSelectedEditOptionChanged(): Boolean = when (editOption) {
        EditOption.ROTATE -> edit.rotationDeg != 0f
        EditOption.EXPOSURE -> edit.exposure != 0f
        EditOption.BRILLIANCE -> edit.brilliance != 0f
        EditOption.BRIGHTNESS -> edit.brightness != 0f
        EditOption.HIGHLIGHTS -> edit.highlights != 0f
        EditOption.SHADOWS -> edit.shadows != 0f
        EditOption.CONTRAST -> edit.contrast != 0f
        EditOption.SATURATION -> edit.saturation != 1f
        EditOption.VIBRANCY -> edit.vibrancy != 0f
        EditOption.WARMTH -> edit.warmth != 0f
        EditOption.VIGNETTE -> edit.vignette != 0f
        EditOption.SHARPNESS -> edit.sharpness != 0f
    }

    fun EditState.resetOption(option: EditOption): EditState = when (option) {
        EditOption.ROTATE -> copy(rotationDeg = 0f)
        EditOption.EXPOSURE -> copy(exposure = 0f)
        EditOption.BRILLIANCE -> copy(brilliance = 0f)
        EditOption.BRIGHTNESS -> copy(brightness = 0f)
        EditOption.HIGHLIGHTS -> copy(highlights = 0f)
        EditOption.SHADOWS -> copy(shadows = 0f)
        EditOption.CONTRAST -> copy(contrast = 0f)
        EditOption.SATURATION -> copy(saturation = 1f)
        EditOption.VIBRANCY -> copy(vibrancy = 0f)
        EditOption.WARMTH -> copy(warmth = 0f)
        EditOption.VIGNETTE -> copy(vignette = 0f)
        EditOption.SHARPNESS -> copy(sharpness = 0f)
    }

    fun undoCurrentEditOption() {
        editRedoStack.addLast(edit)
        val resetState = edit.resetOption(editOption)
        while (editUndoStack.isNotEmpty()) {
            val last = editUndoStack.last()
            if (last.resetOption(editOption) == resetState) {
                editUndoStack.removeLast()
            } else {
                break
            }
        }
        edit = resetState
    }

    fun undoFilter() {
        editRedoStack.addLast(edit)
        val resetState = edit.copy(preset = FilterPreset.ORIGINAL)
        while (editUndoStack.isNotEmpty()) {
            val last = editUndoStack.last()
            if (last.copy(preset = FilterPreset.ORIGINAL) == resetState) {
                editUndoStack.removeLast()
            } else {
                break
            }
        }
        edit = resetState
    }

    fun undoEditorChange() {
        when {
            tool == Tool.DOODLE && vm.undoStack.isNotEmpty() -> vm.undoDoodle()
            openMode == PhotoEditorMode.CROP && vm.cropHistory.isNotEmpty() -> vm.undoCrop()
            openMode == PhotoEditorMode.FILTERS && edit.preset != FilterPreset.ORIGINAL -> undoFilter()
            openMode == PhotoEditorMode.EDIT && isSelectedEditOptionChanged() -> undoCurrentEditOption()
            isSelectedEditOptionChanged() -> undoCurrentEditOption()
            edit.preset != FilterPreset.ORIGINAL -> undoFilter()
            editUndoStack.isNotEmpty() -> {
                val previous = editUndoStack.removeLast()
                if (previous != edit) {
                    editRedoStack.addLast(edit)
                    edit = previous
                }
            }
        }
    }

    fun redoEditorChange() {
        when {
            tool == Tool.DOODLE && vm.redoStack.isNotEmpty() -> vm.redoDoodle()
            openMode == PhotoEditorMode.CROP && vm.cropRedoHistory.isNotEmpty() -> vm.redoCrop()
            editRedoStack.isNotEmpty() -> {
                editUndoStack.addLast(edit)
                edit = editRedoStack.removeLast()
            }
        }
    }

    LaunchedEffect(item.uri) {
        vm.loadPhoto(item.uri.toString())
        showOriginalPreview = false
    }

    LaunchedEffect(tool) {
        showOriginalPreview = false
    }

    var containerW by remember { mutableStateOf(0f) }
    var containerH by remember { mutableStateOf(0f) }

    LaunchedEffect(tool) {
        if (tool == Tool.CROP) {
            previousCropState = edit
        } else {
            vm.setCropRect(null)
            if (previousCropState != null) {
                // Switch away resets crop unless user explicitly clicked Apply
                edit = edit.copy(
                    cropLeft = previousCropState!!.cropLeft,
                    cropTop = previousCropState!!.cropTop,
                    cropRight = previousCropState!!.cropRight,
                    cropBottom = previousCropState!!.cropBottom,
                    cropAspect = previousCropState!!.cropAspect
                )
                previousCropState = null
            }
        }
    }

    // Live preview colour filter from the same matrix the save bake uses.
    val composeMatrix = remember(edit) { ColorMatrix(EditMatrix.combined(edit).array.copyOf()) }
    var sharpPreviewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(workingBmp, edit.sharpness) {
        val bmp = workingBmp
        if (bmp == null || edit.sharpness <= 0f) {
            sharpPreviewBitmap = null
        } else {
            val amount = edit.sharpness
            delay(140)
            sharpPreviewBitmap = withContext(Dispatchers.Default) {
                vm.previewSharpness(bmp, amount)
            }
        }
    }
    val previewBitmap = sharpPreviewBitmap ?: workingBmp

    val isRotatedOdd = ((edit.rotationDeg / 90f).toInt() % 2) != 0
    val baseWidth = workingBmp?.width ?: item.width
    val baseHeight = workingBmp?.height ?: item.height
    val imageRatio = if (isRotatedOdd) {
        baseHeight.toFloat() / baseWidth.toFloat()
    } else {
        baseWidth.toFloat() / baseHeight.toFloat()
    }

    val loadError = vm.loadError.value

    val editorBackdrop = rememberLayerBackdrop()

    CompositionLocalProvider(LocalLiquidGlassContentBackdrop provides editorBackdrop) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
        if (loadError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                // Top-left Cancel button matching the design system
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    CirclePill(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Cancel",
                            tint = DesignTokens.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Centered error card with retry button
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.85f)
                        .kernelSuGlassBackdrop(
                            shape = RoundedCornerShape(DesignTokens.RadiusCard)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Couldn't open this photo",
                            color = DesignTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Box(
                            modifier = Modifier.kernelSuGlassBackdrop(
                                shape = RoundedCornerShape(percent = 50)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .pressScale(scale = 0.94f)
                                    .clickable { vm.loadPhoto(item.uri.toString()) }
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val originalOnClose = onClose
            var uiVisible by remember { mutableStateOf(false) }
            val closeScope = androidx.compose.runtime.rememberCoroutineScope()
            val onClose: () -> Unit = {
                if (uiVisible) {
                    uiVisible = false
                    closeScope.launch {
                        kotlinx.coroutines.delay(250)
                        originalOnClose()
                    }
                } else {
                    originalOnClose()
                }
            }
            LaunchedEffect(Unit) { uiVisible = true }
            val topOffset by androidx.compose.animation.core.animateDpAsState(if (uiVisible) 0.dp else (-100).dp, androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            val bottomOffset by androidx.compose.animation.core.animateDpAsState(if (uiVisible) 0.dp else 100.dp, androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            val uiAlpha by androidx.compose.animation.core.animateFloatAsState(if (uiVisible) 1f else 0f, androidx.compose.animation.core.tween(400))

            Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
                    .offset(y = topOffset)
                    .alpha(uiAlpha)
                    .statusBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            val undoRedoPill = @Composable {
                PhotoUndoRedoPill(
                    canUndo = when {
                        tool == Tool.DOODLE -> vm.undoStack.isNotEmpty()
                        openMode == PhotoEditorMode.CROP -> vm.cropHistory.isNotEmpty()
                        else -> isSelectedEditOptionChanged() || edit.preset != FilterPreset.ORIGINAL || editUndoStack.isNotEmpty()
                    },
                    canRedo = when {
                        tool == Tool.DOODLE -> vm.redoStack.isNotEmpty()
                        openMode == PhotoEditorMode.CROP -> vm.cropRedoHistory.isNotEmpty()
                        else -> editRedoStack.isNotEmpty()
                    },
                    onUndo = ::undoEditorChange,
                    onRedo = ::redoEditorChange,
                    modifier = Modifier,
                )
            }

            if (isLandscape) {
                PhotoEditorTopChrome(
                    saving = saving,
                    onCancel = onClose,
                    onDone = { showSaveOptions = true },
                    modifier = Modifier,
                    centerContent = undoRedoPill,
                )
            } else {
                PhotoEditorTopChrome(
                    saving = saving,
                    onCancel = onClose,
                    onDone = { showSaveOptions = true },
                    modifier = Modifier,
                )
                undoRedoPill()
            }
            }

            // Preview (fills remaining space).
            val zoomableState = me.saket.telephoto.zoomable.rememberZoomableState(
                zoomSpec = me.saket.telephoto.zoomable.ZoomSpec(maxZoomFactor = 10f)
            ).apply { contentScale = androidx.compose.ui.layout.ContentScale.None }
            
            LaunchedEffect(tool, item.uri) {
                zoomableState.resetZoom(withAnimation = false)
            }

            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val isZoomed by androidx.compose.runtime.remember {
                androidx.compose.runtime.derivedStateOf {
                    zoomableState.contentTransformation.isSpecified &&
                    zoomableState.contentTransformation.scaleMetadata.userZoom > 1.05f
                }
            }
            androidx.activity.compose.BackHandler {
                if (isZoomed) {
                    scope.launch { zoomableState.resetZoom(withAnimation = true) }
                } else {
                    onClose()
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .layerBackdrop(editorBackdrop),
                contentAlignment = Alignment.Center
            ) {
                val fullW = maxWidth.value
                val fullH = maxHeight.value

                val startPad = if (isLandscape) 120f else 32f
                val endPad = if (isLandscape) 120f else 32f
                val topPad = if (isLandscape) 32f else 120f
                val bottomPad = if (isLandscape) 32f else 220f

                val parentW = fullW - startPad - endPad
                val parentH = fullH - topPad - bottomPad

                if (parentW > 0f && parentH > 0f) {
                    val parentAspect = parentW / parentH
                    val boxW = if (imageRatio > parentAspect) parentW else (parentH * imageRatio)
                    val boxH = if (imageRatio > parentAspect) (parentW / imageRatio) else parentH

                    val density = androidx.compose.ui.platform.LocalDensity.current
                    LaunchedEffect(boxW, boxH, fullW, fullH) {
                        if (boxW > 0f && boxH > 0f) {
                            zoomableState.setContentLocation(
                                object : me.saket.telephoto.zoomable.ZoomableContentLocation {
                                    override fun location(
                                        layoutSize: androidx.compose.ui.geometry.Size,
                                        direction: androidx.compose.ui.unit.LayoutDirection
                                    ): androidx.compose.ui.geometry.Rect {
                                        val startPx = with(density) { startPad.dp.toPx() }
                                        val endPx = with(density) { endPad.dp.toPx() }
                                        val topPx = with(density) { topPad.dp.toPx() }
                                        val bottomPx = with(density) { bottomPad.dp.toPx() }

                                        val padW = layoutSize.width - startPx - endPx
                                        val padH = layoutSize.height - topPx - bottomPx

                                        val wPx = with(density) { boxW.dp.toPx() }
                                        val hPx = with(density) { boxH.dp.toPx() }

                                        val imgX = startPx + (padW - wPx) / 2f
                                        val imgY = topPx + (padH - hPx) / 2f

                                        return androidx.compose.ui.geometry.Rect(
                                            left = imgX,
                                            top = imgY,
                                            right = imgX + wPx,
                                            bottom = imgY + hPx
                                        )
                                    }
                                }
                            )

                            val startPx = with(density) { startPad.dp.toPx() }
                            val endPx = with(density) { endPad.dp.toPx() }
                            val topPx = with(density) { topPad.dp.toPx() }
                            val bottomPx = with(density) { bottomPad.dp.toPx() }
                            
                            val fullWPx = with(density) { fullW.dp.toPx() }
                            val fullHPx = with(density) { fullH.dp.toPx() }

                            val padW = fullWPx - startPx - endPx
                            val padH = fullHPx - topPx - bottomPx
                            val wPx = with(density) { boxW.dp.toPx() }
                            val hPx = with(density) { boxH.dp.toPx() }
                            val imgX = startPx + (padW - wPx) / 2f
                            val imgY = topPx + (padH - hPx) / 2f

                            zoomableState.contentAlignment = object : androidx.compose.ui.Alignment {
                                override fun align(
                                    size: androidx.compose.ui.unit.IntSize,
                                    space: androidx.compose.ui.unit.IntSize,
                                    layoutDirection: androidx.compose.ui.unit.LayoutDirection
                                ): androidx.compose.ui.unit.IntOffset {
                                    val progressX = if (space.width > wPx) {
                                        (size.width.toFloat() - wPx) / (space.width - wPx)
                                    } else 0f
                                    
                                    val progressY = if (space.height > hPx) {
                                        (size.height.toFloat() - hPx) / (space.height - hPx)
                                    } else 0f
                                    
                                    val clampedX = progressX.coerceIn(0f, 1f)
                                    val clampedY = progressY.coerceIn(0f, 1f)
                                    
                                    val targetX = imgX * (1f - clampedX)
                                    val targetY = imgY * (1f - clampedY)
                                    
                                    return androidx.compose.ui.unit.IntOffset(targetX.toInt(), targetY.toInt())
                                }
                            }
                        }
                    }

                    val zoomModifier = if (tool != Tool.CROP) {
                        Modifier.zoomable(
                            state = zoomableState,
                            onDoubleClick = me.saket.telephoto.zoomable.DoubleClickToZoomListener.cycle(2.5f),
                            clipToBounds = false
                        )
                    } else Modifier

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(zoomModifier)
                            .padding(
                                start = startPad.dp,
                                end = endPad.dp,
                                top = topPad.dp,
                                bottom = bottomPad.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(boxW.dp, boxH.dp)
                                    .then(
                                        if (tool != Tool.CROP && tool != Tool.DOODLE) {
                                            Modifier.pressAndHoldOriginalPreview(
                                                onShowOriginalChange = { showOriginalPreview = it }
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .onGloballyPositioned { coords ->
                                        val w = coords.size.width
                                        val h = coords.size.height
                                        if (w > 0 && h > 0) {
                                            vm.initOverlay(w, h)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                            LaunchedEffect(boxW, boxH) {
                                containerW = boxW
                                containerH = boxH
                            }

                            LaunchedEffect(tool, boxW, boxH) {
                                if (tool == Tool.CROP && vm.cropRect.value == null) {
                                    val initial = androidx.compose.ui.geometry.Rect(
                                        left = edit.cropLeft * boxW,
                                        top = edit.cropTop * boxH,
                                        right = edit.cropRight * boxW,
                                        bottom = edit.cropBottom * boxH
                                    )
                                    vm.setCropRect(initial)
                                }
                            }

                            if (showOriginalPreview) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.uri)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(false)
                                        .build(),
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else previewBitmap?.let { bmp ->
                                    androidx.compose.foundation.Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = item.displayName,
                                        contentScale = ContentScale.Fit,
                                        colorFilter = ColorFilter.colorMatrix(composeMatrix),
                                        modifier = Modifier
                                            .requiredSize(
                                                width = if (isRotatedOdd) boxH.dp else boxW.dp,
                                                height = if (isRotatedOdd) boxW.dp else boxH.dp
                                            )
                                            .scale(scaleX = if (edit.flipHorizontal) -1f else 1f, scaleY = 1f)
                                            .rotate(edit.rotationDeg)
                                            .onGloballyPositioned { coords ->
                                                val containerSize = coords.size
                                                val isRotatedOdd = ((edit.rotationDeg / 90f).toInt() % 2) != 0
                                                val bmpW = if (isRotatedOdd) bmp.height else bmp.width
                                                val bmpH = if (isRotatedOdd) bmp.width else bmp.height

                                                val bitmapAspect = bmpW.toFloat() / bmpH.toFloat()
                                                val containerAspect = containerSize.width.toFloat() / containerSize.height.toFloat()

                                                val renderedWidth: Float
                                                val renderedHeight: Float
                                                if (bitmapAspect > containerAspect) {
                                                    renderedWidth = containerSize.width.toFloat()
                                                    renderedHeight = containerSize.width / bitmapAspect
                                                } else {
                                                    renderedHeight = containerSize.height.toFloat()
                                                    renderedWidth = containerSize.height * bitmapAspect
                                                }

                                                val offsetX = (containerSize.width - renderedWidth) / 2f
                                                val offsetY = (containerSize.height - renderedHeight) / 2f
                                                val topLeft = Offset(offsetX, offsetY)

                                                vm.imageRenderedBounds = androidx.compose.ui.geometry.Rect(
                                                    offset = topLeft,
                                                    size = androidx.compose.ui.geometry.Size(renderedWidth, renderedHeight)
                                                )
                                            },
                                    )
                                } ?: AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.uri)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.colorMatrix(composeMatrix),
                                modifier = Modifier
                                    .requiredSize(
                                        width = if (isRotatedOdd) boxH.dp else boxW.dp,
                                        height = if (isRotatedOdd) boxW.dp else boxH.dp
                                    )
                                    .scale(scaleX = if (edit.flipHorizontal) -1f else 1f, scaleY = 1f)
                                    .rotate(edit.rotationDeg)
                            )

                            if (!showOriginalPreview && edit.vignette > 0f) {
                                VignettePreview(
                                    amount = edit.vignette,
                                    modifier = Modifier
                                        .requiredSize(
                                            width = if (isRotatedOdd) boxH.dp else boxW.dp,
                                            height = if (isRotatedOdd) boxW.dp else boxH.dp
                                        )
                                        .scale(scaleX = if (edit.flipHorizontal) -1f else 1f, scaleY = 1f)
                                        .rotate(edit.rotationDeg),
                                )
                            }

                            if (!showOriginalPreview && tool == Tool.CROP) {
                                CropOverlay(
                                    cropAspect = edit.cropAspect,
                                    cropRect = vm.cropRect.value,
                                    onCropRectChange = { vm.setCropRect(it) },
                                    imageWidth = boxW,
                                    imageHeight = boxH
                                )
                            }

                            DoodleCanvas(
                                vm = vm,
                                isInteractive = (tool == Tool.DOODLE && !showOriginalPreview),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = if (showOriginalPreview) 0f else 1f }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = bottomOffset)
                    .alpha(uiAlpha)
                    .navigationBarsPadding()
                    .padding(
                        start = if (isLandscape) 88.dp else 28.dp,
                        end = if (isLandscape) 88.dp else 28.dp,
                        top = 10.dp,
                        bottom = 14.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isLandscape) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = openMode != null,
                        enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) + androidx.compose.animation.fadeOut()
                    ) {
                        val visibleMode = openMode ?: mode
                        PhotoEditorPanel(
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.widthIn(max = 438.dp),
                        ) {
                            Column(
                                Modifier.padding(vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                when (visibleMode) {
                                    PhotoEditorMode.EDIT -> {
                                        Box(Modifier.padding(horizontal = 10.dp)) {
                                            EditValueControl(
                                                selected = editOption,
                                                edit = edit,
                                                onEditChange = ::setEditState,
                                            )
                                        }
                                        EditOptionSelector(
                                            selected = editOption,
                                            onSelect = { editOption = it },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                        )
                                    }
                                    PhotoEditorMode.DOODLE -> {
                                        DoodleControls(
                                            vm = vm,
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 10.dp)
                                        )
                                    }
                                    PhotoEditorMode.FILTERS -> {
                                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight().softHorizontalScrollEdges(listState),
                                            state = listState,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically,
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                                        ) {
                                            items(FilterPreset.entries) { preset ->
                                                FilterChip(
                                                    preset = preset,
                                                    selected = edit.preset == preset,
                                                    imageUri = item.uri,
                                                    onClick = { setEditState(edit.copy(preset = preset)) }
                                                )
                                            }
                                        }
                                    }
                                    PhotoEditorMode.CROP -> {
                                        Column(
                                            Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max).padding(horizontal = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Row(
                                                Modifier.padding(horizontal = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                CropAspect.entries.filter { it != CropAspect.ORIGINAL }.forEach { aspect ->
                                                    FeaturePill(
                                                        icon = { tint ->
                                                            Icon(
                                                                Icons.Rounded.Crop,
                                                                contentDescription = null,
                                                                tint = tint,
                                                                modifier = Modifier.size(16.dp),
                                                            )
                                                        },
                                                        label = aspect.label,
                                                        selected = edit.cropAspect == aspect,
                                                    ) {
                                                        setEditState(edit.copy(cropAspect = aspect))
                                                        val currentRect = vm.cropRect.value
                                                        if (currentRect != null && containerW > 0 && containerH > 0) {
                                                            val snapped = snapToAspect(
                                                                current = currentRect,
                                                                aspect = aspect,
                                                                imageWidth = containerW,
                                                                imageHeight = containerH
                                                            )
                                                            vm.setCropRect(snapped)
                                                        }
                                                    }
                                                }
                                            }
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                val isOverlayModified = vm.cropRect.value?.let { rect ->
                                                    rect.left > 1f || rect.top > 1f || rect.right < containerW - 1f || rect.bottom < containerH - 1f
                                                } ?: false
                                                val canReset = edit.cropAspect != CropAspect.FREE || isOverlayModified
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
                                                    onClick = {
                                                        setEditState(edit.copy(cropAspect = CropAspect.FREE))
                                                        vm.setCropRect(androidx.compose.ui.geometry.Rect(0f, 0f, containerW, containerH))
                                                    }
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
                                                    label = "Apply ${edit.cropAspect.label}",
                                                    selected = true,
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        val currentRect = vm.cropRect.value
                                                        if (currentRect != null && containerW > 0f && containerH > 0f) {
                                                            vm.applyCrop(
                                                                cropLeft = (currentRect.left / containerW).coerceIn(0f, 1f),
                                                                cropTop = (currentRect.top / containerH).coerceIn(0f, 1f),
                                                                cropRight = (currentRect.right / containerW).coerceIn(0f, 1f),
                                                                cropBottom = (currentRect.bottom / containerH).coerceIn(0f, 1f),
                                                                onSuccess = {
                                                                    previousCropState = null
                                                                    tool = null
                                                                    mode = PhotoEditorMode.EDIT
                                                                    openMode = PhotoEditorMode.EDIT
                                                                },
                                                                onFailure = { msg ->
                                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        } else {
                                                            android.widget.Toast.makeText(context, "Select a crop area first", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    PhotoEditorPanel(
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                    ) {
                        Column(
                            Modifier.padding(top = 10.dp, bottom = 14.dp),
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = openMode != null,
                                enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) + androidx.compose.animation.fadeOut()
                            ) {
                                val visibleMode = openMode ?: mode
                                Column(
                                    modifier = Modifier.padding(bottom = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    when (visibleMode) {
                                        PhotoEditorMode.EDIT -> {
                                        Box(Modifier.padding(horizontal = 10.dp)) {
                                            EditValueControl(
                                                selected = editOption,
                                                edit = edit,
                                                onEditChange = ::setEditState,
                                            )
                                        }
                                        EditOptionSelector(
                                            selected = editOption,
                                            onSelect = { editOption = it },
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                        )
                                    }
                                    PhotoEditorMode.DOODLE -> {
                                        DoodleControls(
                                            vm = vm,
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 10.dp)
                                        )
                                    }
                                    PhotoEditorMode.FILTERS -> {
                                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight().softHorizontalScrollEdges(listState),
                                            state = listState,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically,
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                                        ) {
                                            items(FilterPreset.entries) { preset ->
                                                FilterChip(
                                                    preset = preset,
                                                    selected = edit.preset == preset,
                                                    imageUri = item.uri,
                                                    onClick = { setEditState(edit.copy(preset = preset)) }
                                                )
                                            }
                                        }
                                    }
                                    PhotoEditorMode.CROP -> {
                                        Column(
                                            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                                CropAspect.entries.filter { it != CropAspect.ORIGINAL }.forEach { aspect ->
                                                    FeaturePill(
                                                        icon = { tint ->
                                                            Icon(
                                                                Icons.Rounded.Crop,
                                                                contentDescription = null,
                                                                tint = tint,
                                                                modifier = Modifier.size(16.dp),
                                                            )
                                                        },
                                                        label = aspect.label,
                                                        selected = edit.cropAspect == aspect,
                                                    ) {
                                                        setEditState(edit.copy(cropAspect = aspect))
                                                        val currentRect = vm.cropRect.value
                                                        if (currentRect != null && containerW > 0 && containerH > 0) {
                                                            val snapped = snapToAspect(
                                                                current = currentRect,
                                                                aspect = aspect,
                                                                imageWidth = containerW,
                                                                imageHeight = containerH
                                                            )
                                                            vm.setCropRect(snapped)
                                                        }
                                                    }
                                                }
                                            }
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                val isOverlayModified = vm.cropRect.value?.let { rect ->
                                                    rect.left > 1f || rect.top > 1f || rect.right < containerW - 1f || rect.bottom < containerH - 1f
                                                } ?: false
                                                val canReset = edit.cropAspect != CropAspect.FREE || isOverlayModified
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
                                                    onClick = {
                                                        setEditState(edit.copy(cropAspect = CropAspect.FREE))
                                                        vm.setCropRect(androidx.compose.ui.geometry.Rect(0f, 0f, containerW, containerH))
                                                    }
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
                                                    label = "Apply ${edit.cropAspect.label}",
                                                    selected = true,
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        val currentRect = vm.cropRect.value
                                                        if (currentRect != null && containerW > 0f && containerH > 0f) {
                                                            vm.applyCrop(
                                                                cropLeft = (currentRect.left / containerW).coerceIn(0f, 1f),
                                                                cropTop = (currentRect.top / containerH).coerceIn(0f, 1f),
                                                                cropRight = (currentRect.right / containerW).coerceIn(0f, 1f),
                                                                cropBottom = (currentRect.bottom / containerH).coerceIn(0f, 1f),
                                                                onSuccess = {
                                                                    previousCropState = null
                                                                    tool = null
                                                                    mode = PhotoEditorMode.EDIT
                                                                    openMode = PhotoEditorMode.EDIT
                                                                },
                                                                onFailure = { msg ->
                                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        } else {
                                                            android.widget.Toast.makeText(context, "Select a crop area first", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    }
                            }
                        }
                            
                            PhotoModeDock(
                                selected = mode,
                                isMenuOpen = openMode != null,
                                onSelect = { selectedMode ->
                                    if (selectedMode == mode && openMode == selectedMode) {
                                        openMode = null
                                        tool = null
                                    } else {
                                        mode = selectedMode
                                        openMode = selectedMode
                                        tool = when (selectedMode) {
                                            PhotoEditorMode.DOODLE -> Tool.DOODLE
                                            PhotoEditorMode.CROP -> Tool.CROP
                                            else -> null
                                        }
                                    }
                                },
                            )
                        }
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
                PhotoModeDockVertical(
                    selected = mode,
                    onSelect = { selectedMode ->
                        if (selectedMode == mode && openMode == selectedMode) {
                            openMode = null
                            tool = null
                        } else {
                            mode = selectedMode
                            openMode = selectedMode
                            tool = when (selectedMode) {
                                PhotoEditorMode.DOODLE -> Tool.DOODLE
                                PhotoEditorMode.CROP -> Tool.CROP
                                else -> null
                            }
                        }
                    },
                )
            }
        }
        }

    }

    if (showSaveOptions) {
        GlassCustomDialog(
            onDismissRequest = { showSaveOptions = false }
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Save photo?", style = MaterialTheme.typography.titleLarge, color = DesignTokens.TextPrimary)
                Text("Save a new edited copy, or overwrite the original file.", style = MaterialTheme.typography.bodyMedium, color = DesignTokens.TextSecondary, textAlign = TextAlign.Center)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.Button(
                        onClick = {
                            showSaveOptions = false
                            vm.saveCopy(app, item, edit) { onClose() }
                        },
                        modifier = Modifier.weight(1f).pressScale(scale = 0.92f)
                    ) {
                        Text("Save copy", textAlign = TextAlign.Center)
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            showSaveOptions = false
                            confirmOverwrite = true
                        },
                        modifier = Modifier.weight(1f).pressScale(scale = 0.92f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.2f), contentColor = Color.Red)
                    ) {
                        Text("Overwrite", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    if (confirmOverwrite) {
        GlassAlertDialog(
            onDismissRequest = { confirmOverwrite = false },
            title = "Overwrite original?",
            text = "This replaces the original file. This can't be undone.",
            confirmLabel = "Overwrite",
            onConfirm = {
                confirmOverwrite = false
                vm.overwrite(app, item, edit) { onClose() }
            },
            dismissLabel = "Cancel",
            onDismiss = { confirmOverwrite = false }
        )
    }
    }
}

@Composable
private fun PhotoEditorTopChrome(
    saving: Boolean,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    centerContent: (@Composable () -> Unit)? = null,
) {
    Box(modifier.widthIn(max = 520.dp).fillMaxWidth()) {
        PhotoHeaderButton(
            label = "Cancel",
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(96.dp),
        )

        if (centerContent != null) {
            Box(Modifier.align(Alignment.Center)) {
                centerContent()
            }
        } else {
            Text(
                "PHOTO",
                color = Color.White.copy(alpha = 0.52f),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        PhotoHeaderButton(
            label = if (saving) "" else "Done",
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
private fun PhotoHeaderButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentFill: Boolean = false,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit = {},
) {
    val accent = LocalDynamicColors.current.accent
    PhotoEditorPanel(
        shape = CapsuleShape,
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
fun PhotoUndoRedoPill(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PhotoEditorPanel(
        shape = CapsuleShape,
        modifier = modifier
            .width(82.dp)
            .height(34.dp),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(CapsuleShape)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            HeaderPillIcon(
                enabled = canUndo,
                onClick = onUndo,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo",
                    tint = Color.White.copy(alpha = if (canUndo) 0.94f else 0.30f),
                    modifier = Modifier.size(19.dp),
                )
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(16.dp)
                    .background(com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent.copy(alpha = 0.12f)),
            )
            HeaderPillIcon(
                enabled = canRedo,
                onClick = onRedo,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = "Redo",
                    tint = Color.White.copy(alpha = if (canRedo) 0.94f else 0.30f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderPillIcon(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(width = 34.dp, height = 30.dp)
            .clip(CapsuleShape)
            .pressScale(scale = 0.88f, enabled = enabled)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PhotoHeaderIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PhotoEditorPanel(
        shape = CapsuleShape,
        modifier = modifier
            .size(44.dp)
            .pressScale(scale = 0.88f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CapsuleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun PhotoEditorPanel(
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    enableLens: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.kernelSuGlassBackdrop(
            shape = shape,
            enableLens = enableLens,
        ),
    ) {
        content()
    }
}

@Composable
private fun PhotoModeDock(
    selected: PhotoEditorMode,
    isMenuOpen: Boolean,
    onSelect: (PhotoEditorMode) -> Unit,
) {
    val modes = PhotoEditorMode.entries
    val density = LocalDensity.current
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)
    val selectedIndexState by rememberUpdatedState(selectedIndex)
    val onSelectState by rememberUpdatedState(onSelect)
    val itemSpacing = 2.dp
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

    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = isMenuOpen,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f))
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
        }
        Box(
            Modifier
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = if (isMenuOpen) 14.dp else 4.dp,
                    bottom = 0.dp
                )
                .fillMaxWidth()
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
            var pressedMode by remember { mutableStateOf<PhotoEditorMode?>(null) }
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
                    label = "photoEditorModePillOffset",
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
                    PhotoDockButton(
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
private fun PhotoDockButton(
    mode: PhotoEditorMode,
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
private fun PhotoModeDockVertical(
    selected: PhotoEditorMode,
    onSelect: (PhotoEditorMode) -> Unit,
) {
    val modes = PhotoEditorMode.entries
    val density = LocalDensity.current
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)
    val selectedIndexState by rememberUpdatedState(selectedIndex)
    val onSelectState by rememberUpdatedState(onSelect)
    val itemSpacing = 10.dp
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

    PhotoEditorPanel(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.heightIn(max = 330.dp),
    ) {
        Box(
            Modifier
                .padding(4.dp)
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
            var pressedMode by remember { mutableStateOf<PhotoEditorMode?>(null) }
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
                    label = "photoEditorModePillOffsetVertical",
                )
                val activeBgColor = accent.copy(alpha = if (appColors.isDark) 0.22f else 0.26f)

                Box(
                    Modifier
                        .offset(y = selectedOffsetDp)
                        .scale(bgScale)
                        .size(width = 64.dp, height = selectedHeightDp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(activeBgColor)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (appColors.isDark) 0.24f else 0.48f),
                            shape = RoundedCornerShape(24.dp),
                        ),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.zIndex(1f),
            ) {
                modes.forEach { mode ->
                    PhotoDockButtonVertical(
                        mode = mode,
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.height(if (colHeightPx > 0) with(density) { segmentHeightPx().toDp() } else 56.dp),
                        onPressedChange = { if (it) pressedMode = mode else if (pressedMode == mode) pressedMode = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoDockButtonVertical(
    mode: PhotoEditorMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPressedChange: (Boolean) -> Unit = {}
) {
    val accent = LocalDynamicColors.current.accent
    val tint = if (selected) Color.White else Color.White.copy(alpha = 0.46f)
    Column(
        modifier
            .width(64.dp)
            .clip(RoundedCornerShape(24.dp))
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
            modifier = Modifier.size(24.dp), 
            tint = if (selected) accent else tint
        )
        Spacer(Modifier.height(2.dp))
        Text(mode.label, color = tint, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
    }
}

@Composable
private fun RotateControlRow(
    rotationDeg: Float,
    onRotationChange: (Float) -> Unit,
    onFlip: () -> Unit,
    onRotateRight: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(52.dp)
                .pressScale(scale = 0.88f)
                .clip(CapsuleShape)
                .background(com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CapsuleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFlip,
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Flip,
                contentDescription = "Flip",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        GlassSlider(
            value = rotationDeg,
            onValueChange = onRotationChange,
            valueRange = -180f..180f,
            neutralValue = 0f,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(52.dp)
                .pressScale(scale = 0.88f)
                .clip(CapsuleShape)
                .background(com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CapsuleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRotateRight,
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Rotate90DegreesCw,
                contentDescription = "Rotate right",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EditOptionSelector(
    selected: EditOption,
    onSelect: (EditOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = EditOption.entries
    val accent = LocalDynamicColors.current.accent

    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .softHorizontalScrollEdges(scrollState, fadeWidth = 24.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .height(32.dp)
                    .pressScale(scale = 0.94f)
                    .clip(CapsuleShape)
                    .background(if (isSelected) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f),
                        shape = CapsuleShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(option) },
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    color = if (isSelected) accent else Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EditValueControl(
    selected: EditOption,
    edit: EditState,
    onEditChange: (EditState) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(38.dp),
        contentAlignment = Alignment.Center,
    ) {
            if (selected == EditOption.ROTATE) {
                RotateControlRow(
                    rotationDeg = edit.rotationDeg,
                    onRotationChange = { onEditChange(edit.copy(rotationDeg = it)) },
                    onFlip = {
                        onEditChange(edit.copy(flipHorizontal = !edit.flipHorizontal))
                    },
                    onRotateRight = {
                        var newRot = edit.rotationDeg + 90f
                        while (newRot < -180f) newRot += 360f
                        while (newRot > 180f) newRot -= 360f
                        onEditChange(edit.copy(rotationDeg = newRot))
                    },
                )
            } else {
                val sliderValue = when (selected) {
                    EditOption.EXPOSURE -> edit.exposure
                    EditOption.BRILLIANCE -> edit.brilliance
                    EditOption.BRIGHTNESS -> edit.brightness
                    EditOption.HIGHLIGHTS -> edit.highlights
                    EditOption.SHADOWS -> edit.shadows
                    EditOption.CONTRAST -> edit.contrast
                    EditOption.SATURATION -> edit.saturation
                    EditOption.VIBRANCY -> edit.vibrancy
                    EditOption.WARMTH -> edit.warmth
                    EditOption.VIGNETTE -> edit.vignette
                    EditOption.SHARPNESS -> edit.sharpness
                    EditOption.ROTATE -> edit.rotationDeg
                }
                val sliderRange = when (selected) {
                    EditOption.SATURATION -> 0f..2f
                    else -> -1f..1f
                }
                AdjustmentSlider(
                    value = sliderValue,
                    onValueChange = { value ->
                        onEditChange(
                            when (selected) {
                                EditOption.EXPOSURE -> edit.copy(exposure = value)
                                EditOption.BRILLIANCE -> edit.copy(brilliance = value)
                                EditOption.BRIGHTNESS -> edit.copy(brightness = value)
                                EditOption.HIGHLIGHTS -> edit.copy(highlights = value)
                                EditOption.SHADOWS -> edit.copy(shadows = value)
                                EditOption.CONTRAST -> edit.copy(contrast = value)
                                EditOption.SATURATION -> edit.copy(saturation = value)
                                EditOption.VIBRANCY -> edit.copy(vibrancy = value)
                                EditOption.WARMTH -> edit.copy(warmth = value)
                                EditOption.VIGNETTE -> edit.copy(vignette = value)
                                EditOption.SHARPNESS -> edit.copy(sharpness = value)
                                EditOption.ROTATE -> edit
                            }
                        )
                    },
                    valueRange = sliderRange,
                    neutralValue = when (selected) {
                        EditOption.SATURATION -> 1f
                        else -> 0f
                    },
                )
            }
        }
}

@Composable
private fun AdjustmentSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    neutralValue: Float,
) {
    GlassSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        neutralValue = neutralValue,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun VignettePreview(
    amount: Float,
    modifier: Modifier = Modifier,
) {
    val alpha = (amount.coerceIn(0f, 1f) * 0.72f)
    Canvas(modifier = modifier) {
        val radius = maxOf(size.width, size.height) * 0.74f
        drawRect(
            brush = GraphicsBrush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.56f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = alpha),
                ),
                center = center,
                radius = radius,
            ),
            size = size,
        )
    }
}

@Composable
private fun ToolChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(interaction, pressedScale = 0.94f)
            .clickable(interaction, indication = null) { haptic(); onClick() }
            .kernelSuGlassBackdrop(shape = RoundedCornerShape(DesignTokens.RadiusChip)),
    ) {
        Text(
            label,
            color = if (selected) accent else DesignTokens.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun FilterChip(
    preset: FilterPreset,
    selected: Boolean,
    imageUri: android.net.Uri,
    onClick: () -> Unit,
) {
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val accent = LocalDynamicColors.current.accent
    val matrix = remember(preset) { ColorMatrix(EditMatrix.preset(preset).array.copyOf()) }
    
    Box(
        modifier = Modifier
            .width(60.dp)
            .pressScale(interaction, pressedScale = 0.95f)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) accent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f))
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(interaction, indication = null) { haptic(); onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .border(
                        width = 1.dp,
                        color = if (selected) accent else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(matrix),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = preset.label,
                color = if (selected) accent else com.gorilla.gallery.ui.theme.DesignTokens.TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = (-0.01).sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun CirclePill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .pressScale(scale = 0.88f)
            .kernelSuGlassBackdrop(shape = shape)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

internal fun Modifier.pressAndHoldOriginalPreview(
    onShowOriginalChange: (Boolean) -> Unit,
): Modifier = pointerInput(onShowOriginalChange) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            val releasedBeforeHold = withTimeoutOrNull(180) {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.none { it.id == down.id && it.pressed }) {
                        return@withTimeoutOrNull true
                    }
                    if (event.changes.count { it.pressed } > 1) {
                        return@withTimeoutOrNull true // Abort hold preview if multi-touch
                    }
                }
                false
            } == true

            if (releasedBeforeHold) {
                continue
            }

            onShowOriginalChange(true)
            do {
                val event = awaitPointerEvent()
                if (event.changes.none { it.id == down.id && it.pressed }) {
                    break
                }
                if (event.changes.count { it.pressed } > 1) {
                    break // Stop showing original if user starts pinching
                }
            } while (event.changes.any { it.pressed })

            onShowOriginalChange(false)
        }
    }
}

@Composable
private fun DoodleCanvas(
    vm: EditorViewModel,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    var canvasCoords by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val view = androidx.compose.ui.platform.LocalView.current
    var currentPath = androidx.compose.runtime.remember { androidx.compose.ui.graphics.Path() }
    var redrawCounter by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var isDrawing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var currentPaint by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.Paint?>(null) }
    
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .onGloballyPositioned { canvasCoords = it }
            .then(if (isInteractive) {
                Modifier.pointerInput(vm.isTextMode.value, vm.floatingTexts.size) {
                if (vm.isTextMode.value) {
                    detectTapGestures { offset ->
                        if (vm.floatingTexts.isNotEmpty()) {
                            vm.isTextMode.value = false
                            vm.activeTextIndex.intValue = -1
                        } else {
                            // no-op, user creates text via bottom bar
                        }
                    }
                } else {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            
                            val paint = androidx.compose.ui.graphics.Paint().apply {
                                val isPencil = vm.activeDoodleTool.value == "PENCIL"
                                color = if (isPencil) vm.currentColor.value.copy(alpha = 0.5f) else vm.currentColor.value
                                strokeWidth = if (isPencil) (vm.currentStrokeWidth.value * 1.5f).coerceAtLeast(2f) else vm.currentStrokeWidth.value
                                style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                strokeJoin = androidx.compose.ui.graphics.StrokeJoin.Round
                                if (vm.currentColor.value == androidx.compose.ui.graphics.Color.Transparent) {
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                                }
                            }
                            
                            var prevPos = down.position
                            var isMultiTouch = false
                            var snapshotSaved = false
                            
                            currentPath.reset()
                            currentPath.moveTo(prevPos.x, prevPos.y)
                            currentPaint = paint
                            isDrawing = true
                            
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.size > 1) {
                                    isMultiTouch = true
                                }
                                
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) break
                                
                                if (!isMultiTouch) {
                                    if (vm.currentColor.value != Color.Unspecified) {
                                        if (!snapshotSaved) {
                                            vm.saveSnapshot()
                                            snapshotSaved = true
                                        }
                                        change.consume()
                                        
                                        val currPos = change.position
                                        // Use quadratic bezier for smooth curves
                                        currentPath.quadraticBezierTo(prevPos.x, prevPos.y, (prevPos.x + currPos.x) / 2, (prevPos.y + currPos.y) / 2)
                                        redrawCounter++
                                        prevPos = currPos
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            
                            if (!isMultiTouch && snapshotSaved) {
                                currentPath.lineTo(prevPos.x, prevPos.y)
                                vm.drawPath(currentPath, paint)
                            }
                            isDrawing = false
                            currentPaint = null
                        }
                    }
                }
            }
            } else Modifier)
    ) {
        val overlay = vm.overlayBitmap.value
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
        ) {
            if (overlay != null) {
                androidx.compose.foundation.Image(
                    bitmap = overlay,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            if (isDrawing && currentPaint != null) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = redrawCounter
                    drawContext.canvas.drawPath(currentPath, currentPaint!!)
                }
            }
        }

        vm.floatingTexts.forEachIndexed { index, ft ->
            androidx.compose.runtime.key(index) {
                var textWidth by remember { mutableStateOf(0) }
                var textHeight by remember { mutableStateOf(0) }
                val density = androidx.compose.ui.platform.LocalDensity.current.density
                val paddingPx = 16f * density
                val isEditingThis = isInteractive && vm.isTextMode.value && vm.activeTextIndex.intValue == index
                val maxW = constraints.maxWidth.toFloat()
                val maxH = constraints.maxHeight.toFloat()
                val maxWdp = maxWidth


                Box(
                    modifier = Modifier
                        .offset { 
                            androidx.compose.ui.unit.IntOffset(
                                (ft.offset.x - textWidth / 2f).toInt(),
                                (ft.offset.y - textHeight / 2f).toInt()
                            )
                        }
                        .graphicsLayer { rotationZ = ft.rotation }
                        .onGloballyPositioned { coords ->
                            textWidth = coords.size.width
                            textHeight = coords.size.height
                        }
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = ft.text,
                        onValueChange = { vm.floatingTexts[index] = ft.copy(text = it) },
                        enabled = isEditingThis,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = ft.color,
                            fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (ft.scale * 10f * 4f).toSp() },
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontFamily = ft.fontFamily,
                            fontWeight = ft.fontWeight
                        ),
                        modifier = Modifier
                            .widthIn(max = maxWdp)
                            .width(androidx.compose.foundation.layout.IntrinsicSize.Max)
                            .padding(16.dp)
                            .then(
                                if (isEditingThis) {
                                    Modifier
                                        .border(1.dp, Color.White, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val old = vm.floatingTexts[index]
                                                val angleRad = Math.toRadians(old.rotation.toDouble())
                                                val cos = kotlin.math.cos(angleRad).toFloat()
                                                val sin = kotlin.math.sin(angleRad).toFloat()
                                                val absCos = kotlin.math.abs(cos)
                                                val absSin = kotlin.math.abs(sin)
                                                val boundingW = textWidth * absCos + textHeight * absSin
                                                val boundingH = textWidth * absSin + textHeight * absCos
                                                val halfW = boundingW / 2f
                                                val halfH = boundingH / 2f
                                                
                                                val screenDx = dragAmount.x * cos - dragAmount.y * sin
                                                val screenDy = dragAmount.x * sin + dragAmount.y * cos
                                                val screenDragAmount = androidx.compose.ui.geometry.Offset(screenDx, screenDy)
                                                
                                                val newOffset = old.offset + screenDragAmount
                                                val margin = 32f * density
                                                val minX = -margin + halfW
                                                val maxX = maxW + margin - halfW
                                                val minY = -margin + halfH
                                                val maxY = maxH + margin - halfH
                                                val clampedX = if (minX <= maxX) newOffset.x.coerceIn(minX, maxX) else maxW / 2f
                                                val clampedY = if (minY <= maxY) newOffset.y.coerceIn(minY, maxY) else maxH / 2f
                                                vm.floatingTexts[index] = old.copy(offset = androidx.compose.ui.geometry.Offset(clampedX, clampedY))
                                            }
                                        }
                                } else {
                                    Modifier
                                        .padding(8.dp)
                                        .then(if (isInteractive) {
                                            Modifier.pointerInput(Unit) {
                                                detectTapGestures {
                                                    vm.isTextMode.value = true
                                                    vm.activeTextIndex.intValue = index
                                                }
                                            }
                                        } else Modifier)
                                }
                            )
                    )
                    
                    if (isEditingThis) {
                        // Delete Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                                .clickable {
                                    vm.floatingTexts.removeAt(index)
                                    vm.activeTextIndex.intValue = -1
                                    vm.isTextMode.value = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Delete Text",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Resize Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val old = vm.floatingTexts[index]
                                        val scaleChange = 1f + (dragAmount.x / 100f)
                                        val newScale = (old.scale * scaleChange).coerceIn(0.5f, 10f)
                                        if (newScale > old.scale) {
                                            if (textWidth - 2 * paddingPx >= maxW ||
                                                textHeight - 2 * paddingPx >= maxH) {
                                                return@detectDragGestures
                                            }
                                        }
                                        vm.floatingTexts[index] = old.copy(scale = newScale)
                                    }
                                }
                        )

                        // Rotate Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(24.dp)
                                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                                .pointerInput(Unit) {
                                    var accumulateRotation = 0f
                                    detectDragGestures(
                                        onDragStart = { 
                                            accumulateRotation = vm.floatingTexts[index].rotation 
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val rotationChange = -(dragAmount.y / 2f) - (dragAmount.x / 2f)
                                        accumulateRotation = (accumulateRotation + rotationChange) % 360f
                                        if (accumulateRotation < 0f) accumulateRotation += 360f
                                        
                                        var snappedRotation = accumulateRotation
                                        val snapThreshold = 10f
                                        if (kotlin.math.abs(accumulateRotation) < snapThreshold || kotlin.math.abs(accumulateRotation - 360f) < snapThreshold) {
                                            snappedRotation = 0f
                                        } else if (kotlin.math.abs(accumulateRotation - 90f) < snapThreshold) {
                                            snappedRotation = 90f
                                        } else if (kotlin.math.abs(accumulateRotation - 180f) < snapThreshold) {
                                            snappedRotation = 180f
                                        } else if (kotlin.math.abs(accumulateRotation - 270f) < snapThreshold) {
                                            snappedRotation = 270f
                                        }
                                        
                                        vm.floatingTexts[index] = vm.floatingTexts[index].copy(rotation = snappedRotation)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.Rotate90DegreesCw,
                                contentDescription = "Rotate Text",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class DoodleSubTool { PEN, PENCIL, ERASER }

@Composable
private fun DoodleToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    isActive: Boolean,
    onClick: () -> Unit,
    isEraserShape: Boolean = false
) {
    val accentColor = com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent
    Box(
        modifier = Modifier
            .size(30.dp, 24.dp)
            .pressScale(scale = 0.94f)
            .clickable(
                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isEraserShape) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(15.dp)) {
                val w = size.width
                val h = size.height
                rotate(45f) {
                    val rectW = w * 0.45f
                    val rectH = h * 0.8f
                    val left = (w - rectW) / 2
                    val top = (h - rectH) / 2
                    val path = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                left = left, top = top, right = left + rectW, bottom = top + rectH,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f)
                            )
                        )
                    }
                    clipPath(path) {
                        drawRect(
                            color = if (isActive) accentColor else Color.White.copy(alpha = 0.55f),
                            topLeft = androidx.compose.ui.geometry.Offset(left, top + rectH * 0.45f),
                            size = androidx.compose.ui.geometry.Size(rectW, rectH * 0.55f)
                        )
                    }
                    drawPath(
                        path = path,
                        color = if (isActive) accentColor else Color.White.copy(alpha = 0.55f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f)
                    )
                }
            }
        } else {
            androidx.compose.material3.Icon(
                imageVector = icon!!,
                contentDescription = null,
                tint = if (isActive) accentColor else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun DoodleControls(
    vm: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val activeColor = vm.currentColor.value
    val strokeWidth = vm.currentStrokeWidth.value
    var lastUsedColor by remember { mutableStateOf(if (activeColor != Color.Transparent && activeColor != Color.Unspecified) activeColor else Color(0xFF34D399)) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showTextSettings by remember { mutableStateOf(false) }
    
    val isEraser = activeColor == Color.Transparent
    var activeTool by remember { mutableStateOf(if (isEraser) DoodleSubTool.ERASER else DoodleSubTool.valueOf(vm.activeDoodleTool.value)) }
    androidx.compose.runtime.LaunchedEffect(activeTool) { vm.activeDoodleTool.value = activeTool.name }

    if (showColorPicker) {
        com.gorilla.gallery.ui.components.ColorPickerDialog(
            initialColor = lastUsedColor,
            onColorSelected = { 
                vm.currentColor.value = it
                lastUsedColor = it
            },
            onDismissRequest = { showColorPicker = false }
        )
    }

    if (showTextSettings) {
        val ft = vm.floatingText.value
        GlassCustomDialog(
            onDismissRequest = { showTextSettings = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Text Settings", style = MaterialTheme.typography.titleMedium, color = Color.White)
                
                Text("Font Style", style = MaterialTheme.typography.labelMedium, color = DesignTokens.TextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fonts = listOf(
                        "Roboto" to CustomRobotoFamily,
                        "Kalam" to CustomKalamFamily,
                        "Zilla Slab" to CustomZillaSlabFamily
                    )
                    fonts.forEach { (name, family) ->
                        val selected = ft?.fontFamily == family
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) LocalDynamicColors.current.accent else Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    ft?.let { vm.floatingText.value = it.copy(fontFamily = family) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text("Font Weight", style = MaterialTheme.typography.labelMedium, color = DesignTokens.TextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val weights = listOf(
                        "Thin" to FontWeight.Thin,
                        "Normal" to FontWeight.Normal,
                        "Bold" to FontWeight.Bold
                    )
                    weights.forEach { (name, weight) ->
                        val selected = ft?.fontWeight == weight
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) LocalDynamicColors.current.accent else Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    ft?.let { vm.floatingText.value = it.copy(fontWeight = weight) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val doodleOptions = listOf(DoodleSubTool.PEN, DoodleSubTool.PENCIL, DoodleSubTool.ERASER)
            val selectedToolIndex = if (vm.isTextMode.value) -1 else doodleOptions.indexOf(activeTool)
            val toolOffsetDp by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (selectedToolIndex >= 0) (4.dp + (32.dp * selectedToolIndex)) else 0.dp,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.68f, stiffness = 380f),
                label = "toolPillOffset"
            )

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CapsuleShape)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val x = offset.x
                                val idx = ((x - 4.dp.toPx()) / 32.dp.toPx()).toInt().coerceIn(0, 2)
                                val tool = doodleOptions[idx]
                                if (activeTool != tool) {
                                    activeTool = tool
                                    vm.isTextMode.value = false
                                    vm.currentColor.value = if (tool == DoodleSubTool.ERASER) Color.Transparent else lastUsedColor
                                }
                            },
                            onHorizontalDrag = { change, _ ->
                                val x = change.position.x
                                val idx = ((x - 4.dp.toPx()) / 32.dp.toPx()).toInt().coerceIn(0, 2)
                                val tool = doodleOptions[idx]
                                if (activeTool != tool) {
                                    activeTool = tool
                                    vm.isTextMode.value = false
                                    vm.currentColor.value = if (tool == DoodleSubTool.ERASER) Color.Transparent else lastUsedColor
                                }
                                change.consume()
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (selectedToolIndex >= 0) {
                    Box(
                        Modifier
                            .offset(x = toolOffsetDp)
                            .size(30.dp, 24.dp)
                            .clip(CapsuleShape)
                            .background(com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent.copy(alpha = 0.16f))
                            .border(1.dp, com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent.copy(alpha = 0.35f), CapsuleShape)
                    )
                }

                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    DoodleToolItem(
                        icon = androidx.compose.material.icons.Icons.Rounded.Edit,
                        isActive = activeTool == DoodleSubTool.PEN && !vm.isTextMode.value,
                        onClick = { 
                            activeTool = DoodleSubTool.PEN
                            vm.isTextMode.value = false
                            vm.currentColor.value = lastUsedColor 
                        }
                    )
                    DoodleToolItem(
                        icon = androidx.compose.material.icons.Icons.Rounded.Brush,
                        isActive = activeTool == DoodleSubTool.PENCIL && !vm.isTextMode.value,
                        onClick = { 
                            activeTool = DoodleSubTool.PENCIL
                            vm.isTextMode.value = false
                            vm.currentColor.value = lastUsedColor 
                        }
                    )
                    DoodleToolItem(
                        icon = null,
                        isActive = activeTool == DoodleSubTool.ERASER && !vm.isTextMode.value,
                        onClick = { 
                            activeTool = DoodleSubTool.ERASER
                            vm.isTextMode.value = false
                            vm.currentColor.value = Color.Transparent 
                        },
                        isEraserShape = true
                    )
                }
            }

            val isTextMode = vm.isTextMode.value
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .pressScale(scale = 0.94f)
                    .clip(CapsuleShape)
                    .background(if (isTextMode) com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
                    .border(1.dp, if (isTextMode) com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f), CapsuleShape)
                    .clickable(
                        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (isTextMode) {
                            showTextSettings = true
                        } else {
                            vm.isTextMode.value = true
                            if (vm.floatingText.value == null) {
                                vm.floatingText.value = FloatingText(
                                    text = "Add text here",
                                    offset = vm.imageRenderedBounds.center,
                                    color = lastUsedColor,
                                    scale = 1f
                                )
                            }
                        }
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.TextFields,
                    contentDescription = null,
                    tint = if (isTextMode) com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Add Text",
                    color = if (isTextMode) com.gorilla.gallery.ui.theme.LocalDynamicColors.current.accent else Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // Bottom Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(32.dp)
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CapsuleShape)
                    .padding(start = 5.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    val effectiveStroke = if (activeTool == DoodleSubTool.PENCIL) strokeWidth * 1.5f else strokeWidth
                    val nibSize = (strokeWidth / 64f * 18f).coerceIn(4f, 18f).dp
                    Box(
                        modifier = Modifier
                            .size(nibSize)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (activeTool == DoodleSubTool.ERASER) Color.White else if (activeTool == DoodleSubTool.PENCIL) lastUsedColor.copy(alpha = 0.5f) else lastUsedColor)
                            .then(if (activeTool == DoodleSubTool.ERASER) Modifier.border(1.dp, Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape) else Modifier)
                    )
                }

                GlassSlider(
                    value = strokeWidth,
                    onValueChange = { vm.currentStrokeWidth.value = it },
                    valueRange = 4f..64f,
                    modifier = Modifier.width(100.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val swatches = listOf(
                    Color(0xFF34D399),
                    Color(0xFFFF3B30),
                    Color(0xFFFFCC00),
                    Color(0xFF007AFF)
                )
                swatches.forEach { color ->
                    val isActive = lastUsedColor == color && vm.currentColor.value == color && activeTool != DoodleSubTool.ERASER && !vm.isTextMode.value
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .scale(if (isActive) 1.18f else 1f)
                            .pressScale(scale = 0.9f)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color)
                            .border(2.dp, if (isActive) Color.White else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                            .clickable(
                                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                            ) {
                                lastUsedColor = color
                                vm.currentColor.value = color
                                if (activeTool == DoodleSubTool.ERASER) activeTool = DoodleSubTool.PEN
                                vm.isTextMode.value = false
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                        .pressScale(scale = 0.9f)
                        .clickable(
                            interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { showColorPicker = true }
                )
            }
        }
    }
}


private fun Modifier.softHorizontalScrollEdges(
    scrollState: androidx.compose.foundation.ScrollState,
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
                brush = GraphicsBrush.horizontalGradient(
                    0f to Color.Transparent, 1f to Color.Black,
                    startX = 0f, endX = fadePx,
                ),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
        if (scrollState.value < scrollState.maxValue) {
            drawRect(
                brush = GraphicsBrush.horizontalGradient(
                    0f to Color.Black, 1f to Color.Transparent,
                    startX = size.width - fadePx, endX = size.width,
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
    }

private fun Modifier.softHorizontalScrollEdges(
    listState: androidx.compose.foundation.lazy.LazyListState,
    fadeWidth: Dp = 48.dp,
): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        val fadePx = fadeWidth.toPx().coerceAtMost(size.width / 2f)
        if (fadePx <= 0f) return@drawWithContent
        
        val canScrollBackward = listState.canScrollBackward
        val canScrollForward = listState.canScrollForward

        if (canScrollBackward) {
            drawRect(
                brush = GraphicsBrush.horizontalGradient(
                    0f to Color.Transparent, 1f to Color.Black,
                    startX = 0f, endX = fadePx,
                ),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
        if (canScrollForward) {
            drawRect(
                brush = GraphicsBrush.horizontalGradient(
                    0f to Color.Black, 1f to Color.Transparent,
                    startX = size.width - fadePx, endX = size.width,
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn,
            )
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
            .then(if (onClick != null && enabled) Modifier.pressScale(scale = 0.94f) else Modifier)
            .clip(CapsuleShape)
            .background(bgColor, CapsuleShape)
            .border(1.dp, borderColor, CapsuleShape)
            .then(
                if (onClick != null) {
                    Modifier
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
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
