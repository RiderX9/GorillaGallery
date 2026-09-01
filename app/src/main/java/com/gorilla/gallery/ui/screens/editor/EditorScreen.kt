package com.gorilla.gallery.ui.screens.editor

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
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
            Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
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
            var previewScale by remember { mutableFloatStateOf(1f) }
            var previewOffset by remember { mutableStateOf(Offset.Zero) }

            LaunchedEffect(tool, item.uri) {
                previewScale = 1f
                previewOffset = Offset.Zero
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
                    modifier = Modifier.fillMaxSize().layerBackdrop(editorBackdrop),
                    contentAlignment = Alignment.Center
                ) {
                    val parentW = maxWidth.value
                    val parentH = maxHeight.value

                    if (parentW > 0f && parentH > 0f) {
                        val parentAspect = parentW / parentH
                        val boxW = if (imageRatio > parentAspect) parentW else (parentH * imageRatio)
                        val boxH = if (imageRatio > parentAspect) (parentW / imageRatio) else parentH

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(tool) {
                                    if (tool != Tool.CROP) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            previewScale = (previewScale * zoom).coerceIn(1f, 10f)
                                            if (previewScale > 1f) {
                                                val maxX = ((boxW * previewScale) - parentW).coerceAtLeast(0f) / 2f
                                                val maxY = ((boxH * previewScale) - parentH).coerceAtLeast(0f) / 2f
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
                                .pointerInput(tool) {
                                    if (tool != Tool.CROP) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                previewScale = 1f
                                                previewOffset = Offset.Zero
                                            }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(boxW.dp, boxH.dp)
                                    .graphicsLayer {
                                        scaleX = previewScale
                                        scaleY = previewScale
                                        translationX = previewOffset.x
                                        translationY = previewOffset.y
                                    }
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
                                                val topLeft = coords.localToWindow(Offset(offsetX, offsetY))

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

                            val overlay = vm.overlayBitmap.value
                            if (!showOriginalPreview && overlay != null && tool != Tool.DOODLE) {
                                androidx.compose.foundation.Image(
                                    bitmap = overlay,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (!showOriginalPreview && tool == Tool.DOODLE) {
                                DoodleCanvas(
                                    vm = vm,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
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
                                            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp)
                                        )
                                    }
                                    PhotoEditorMode.FILTERS -> {
                                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth().height(64.dp).softHorizontalScrollEdges(listState),
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
                                        val scrollState = rememberScrollState()
                                        Row(
                                            Modifier.fillMaxWidth().height(64.dp).softHorizontalScrollEdges(scrollState).horizontalScroll(scrollState),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Spacer(Modifier.width(2.dp))
                                            CropAspect.entries.filter { it != CropAspect.ORIGINAL }.forEach { aspect ->
                                                ToolChip(label = aspect.label, selected = edit.cropAspect == aspect) {
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
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CapsuleShape)
                                                    .background(accent.copy(alpha = 0.18f))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
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
                                                        }
                                                    )
                                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                            ) {
                                                Text("Apply", color = accent, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.width(2.dp))
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
                                            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp)
                                        )
                                    }
                                    PhotoEditorMode.FILTERS -> {
                                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth().height(64.dp).softHorizontalScrollEdges(listState),
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
                                        val scrollState = rememberScrollState()
                                        Row(
                                            Modifier.fillMaxWidth().height(64.dp).softHorizontalScrollEdges(scrollState).horizontalScroll(scrollState),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Spacer(Modifier.width(2.dp))
                                            CropAspect.entries.filter { it != CropAspect.ORIGINAL }.forEach { aspect ->
                                                ToolChip(label = aspect.label, selected = edit.cropAspect == aspect) {
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
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CapsuleShape)
                                                    .background(accent.copy(alpha = 0.18f))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
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
                                                        }
                                                    )
                                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                            ) {
                                                Text("Apply", color = accent, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.width(2.dp))
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

        if (saving) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
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
                    .background(Color.White.copy(alpha = 0.12f)),
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
    val itemSpacing = 2.dp
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
        shape = CapsuleShape,
        modifier = Modifier.heightIn(max = 330.dp),
    ) {
        Box(
            Modifier
                .padding(horizontal = 6.dp, vertical = 9.dp)
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
                        .size(width = 54.dp, height = selectedHeightDp)
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
            .width(54.dp)
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
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(52.dp)
                .pressScale(scale = 0.88f)
                .clip(CapsuleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CapsuleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRotateLeft,
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Rotate90DegreesCcw,
                contentDescription = "Rotate left",
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
                .height(30.dp)
                .width(52.dp)
                .pressScale(scale = 0.88f)
                .clip(CapsuleShape)
                .background(Color.White.copy(alpha = 0.12f))
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
                    .height(30.dp)
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
                    onRotateLeft = {
                        var newRot = edit.rotationDeg - 90f
                        while (newRot < -180f) newRot += 360f
                        while (newRot > 180f) newRot -= 360f
                        onEditChange(edit.copy(rotationDeg = newRot))
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
            .height(64.dp)
            .pressScale(interaction, pressedScale = 0.94f)
            .clickable(interaction, indication = null) { haptic(); onClick() }
            .kernelSuGlassBackdrop(shape = RoundedCornerShape(DesignTokens.RadiusChip))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
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
                color = if (selected) accent else DesignTokens.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        
                        val paint = androidx.compose.ui.graphics.Paint().apply {
                            color = vm.currentColor.value
                            strokeWidth = vm.currentStrokeWidth.value
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
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size > 1) {
                                isMultiTouch = true
                            }
                            
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) break
                            
                            if (!isMultiTouch) {
                                if (!snapshotSaved) {
                                    vm.saveSnapshot()
                                    snapshotSaved = true
                                }
                                change.consume()
                                
                                val currPos = change.position
                                vm.drawSegment(prevPos.x, prevPos.y, currPos.x, currPos.y, paint)
                                prevPos = currPos
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
    ) {
        val overlay = vm.overlayBitmap.value
        if (overlay != null) {
            androidx.compose.foundation.Image(
                bitmap = overlay,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
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
    val colors = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Transparent)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { color ->
                val isSelected = (activeColor == color)
                val isEraser = color == Color.Transparent
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .pressScale(scale = 0.88f)
                        .clip(CircleShape)
                        .background(if (isEraser) Color(0xFF222222) else color)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) LocalDynamicColors.current.accent else DesignTokens.TextSecondary.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable { vm.currentColor.value = color },
                    contentAlignment = Alignment.Center
                ) {
                    if (isEraser) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
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
                                            left = left,
                                            top = top,
                                            right = left + rectW,
                                            bottom = top + rectH,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f)
                                        )
                                    )
                                }
                                clipPath(path) {
                                    drawRect(
                                        color = Color.White,
                                        topLeft = androidx.compose.ui.geometry.Offset(left, top + rectH * 0.45f),
                                        size = androidx.compose.ui.geometry.Size(rectW, rectH * 0.55f)
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = Color.White,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        GlassSlider(
            value = strokeWidth,
            onValueChange = { vm.currentStrokeWidth.value = it },
            valueRange = 4f..64f,
            modifier = Modifier.weight(1f)
        )
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
