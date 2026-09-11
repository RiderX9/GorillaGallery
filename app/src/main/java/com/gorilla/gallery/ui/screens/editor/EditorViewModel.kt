package com.gorilla.gallery.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.gallery.AppContainer
import com.gorilla.gallery.data.model.EditMatrix
import com.gorilla.gallery.data.model.EditState
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.exifinterface.media.ExifInterface


class EditorViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.photoEditorRepository

    val workingBitmap = mutableStateOf<Bitmap?>(null)
    val overlayBitmap = mutableStateOf<ImageBitmap?>(null, neverEqualPolicy())
    private var overlayCanvas: androidx.compose.ui.graphics.Canvas? = null
    val undoStack = mutableStateListOf<ImageBitmap>()
    val redoStack = mutableStateListOf<ImageBitmap>()
    val cropHistory = mutableStateListOf<Bitmap>()
    val cropRedoHistory = mutableStateListOf<Bitmap>()
    val loadError = mutableStateOf(false)

    val currentColor: MutableState<Color> = mutableStateOf(Color.Red)
    val currentStrokeWidth: MutableState<Float> = mutableStateOf(6f)
    private var loadToken = 0L

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _cropRect = androidx.compose.runtime.mutableStateOf<Rect?>(null)
    val cropRect: androidx.compose.runtime.State<Rect?> = _cropRect

    var imageRenderedBounds: Rect = Rect.Zero

    fun setCropRect(rect: Rect?) {
        _cropRect.value = rect
    }

    fun loadOriginalBitmap(uri: android.net.Uri) {
        if (workingBitmap.value == null) {
            viewModelScope.launch {
                workingBitmap.value = repo.decodeSoftware(uri)
            }
        }
    }

    fun loadPhoto(uriString: String) {
        val uri = android.net.Uri.parse(uriString)
        val token = ++loadToken
        loadError.value = false
        viewModelScope.launch(Dispatchers.Main) {
            workingBitmap.value = null
            overlayBitmap.value = null
            _cropRect.value = null
            imageRenderedBounds = Rect.Zero
            undoStack.clear()
            redoStack.clear()
            cropHistory.clear()
            cropRedoHistory.clear()
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = container.context.contentResolver
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, options)
                }

                // Sample down so longest edge ≤ 4096px
                val maxDimension = 4096
                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDimension ||
                       options.outHeight / sampleSize > maxDimension) {
                    sampleSize *= 2
                }

                val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                val bitmap = contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, decodeOptions)
                }

                // Apply EXIF orientation parsing
                val exif = try {
                    contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
                } catch (e: Exception) {
                    null
                }
                val rotation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val rotatedBitmap = when (rotation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> bitmap?.rotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> bitmap?.rotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> bitmap?.rotate(270f)
                    else -> bitmap
                }

                withContext(Dispatchers.Main) {
                    if (token != loadToken) return@withContext
                    workingBitmap.value = rotatedBitmap
                    // Reset editor states to prevent leakage from previous sessions
                    undoStack.clear()
                    redoStack.clear()
                    cropHistory.clear()
                    cropRedoHistory.clear()
                    overlayBitmap.value = null
                    _cropRect.value = null
                    imageRenderedBounds = Rect.Zero // reset bounds on new load
                }
            } catch (e: SecurityException) {
                android.util.Log.e("EditorVM", "No permission to read URI: $uri", e)
                withContext(Dispatchers.Main) {
                    if (token != loadToken) return@withContext
                    loadError.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorVM", "Failed to load photo: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (token != loadToken) return@withContext
                    loadError.value = true
                }
            }
        }
    }

    fun initOverlay(width: Int, height: Int) {
        val old = overlayBitmap.value
        if (old != null && old.width == width && old.height == height) {
            return
        }
        val bmp = ImageBitmap(width, height)
        val canvas = androidx.compose.ui.graphics.Canvas(bmp)
        if (old != null && old.width > 0 && old.height > 0) {
            val scaleMatrix = Matrix().apply {
                val scaleX = width.toFloat() / old.width
                val scaleY = height.toFloat() / old.height
                postScale(scaleX, scaleY)
            }
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            canvas.nativeCanvas.drawBitmap(old.asAndroidBitmap(), scaleMatrix, paint)
        }
        overlayBitmap.value = bmp
        overlayCanvas = canvas
    }

    fun saveSnapshot() {
        val current = overlayBitmap.value ?: return
        if (undoStack.size >= 20) {
            undoStack.removeAt(0)
        }
        val androidBmp = current.asAndroidBitmap()
        val config = androidBmp.config ?: Bitmap.Config.ARGB_8888
        val copiedAndroidBmp = androidBmp.copy(config, true)
        undoStack.add(copiedAndroidBmp.asImageBitmap())
        redoStack.clear()
    }

    fun drawSegment(x1: Float, y1: Float, x2: Float, y2: Float, paint: androidx.compose.ui.graphics.Paint) {
        val canvas = overlayCanvas ?: return
        canvas.drawLine(
            p1 = androidx.compose.ui.geometry.Offset(x1, y1),
            p2 = androidx.compose.ui.geometry.Offset(x2, y2),
            paint = paint
        )
        val bmp = overlayBitmap.value
        if (bmp != null) {
            overlayBitmap.value = bmp.asAndroidBitmap().asImageBitmap()
        }
    }

    fun undoDoodle() {
        if (undoStack.isNotEmpty()) {
            val current = overlayBitmap.value
            if (current != null) {
                val androidBmp = current.asAndroidBitmap()
                val config = androidBmp.config ?: Bitmap.Config.ARGB_8888
                redoStack.add(androidBmp.copy(config, true).asImageBitmap())
            }
            val last = undoStack.removeAt(undoStack.lastIndex)
            overlayBitmap.value = last
            overlayCanvas = androidx.compose.ui.graphics.Canvas(last)
        } else {
            val current = overlayBitmap.value
            if (current != null) {
                val androidBmp = current.asAndroidBitmap()
                val config = androidBmp.config ?: Bitmap.Config.ARGB_8888
                redoStack.add(androidBmp.copy(config, true).asImageBitmap())
                val blank = ImageBitmap(current.width, current.height)
                overlayBitmap.value = blank
                overlayCanvas = androidx.compose.ui.graphics.Canvas(blank)
            }
        }
    }

    fun redoDoodle() {
        if (redoStack.isNotEmpty()) {
            val current = overlayBitmap.value
            if (current != null) {
                val androidBmp = current.asAndroidBitmap()
                val config = androidBmp.config ?: Bitmap.Config.ARGB_8888
                undoStack.add(androidBmp.copy(config, true).asImageBitmap())
            }
            val next = redoStack.removeAt(redoStack.lastIndex)
            overlayBitmap.value = next
            overlayCanvas = androidx.compose.ui.graphics.Canvas(next)
        }
    }

    fun saveCropSnapshot(bitmap: Bitmap) {
        if (cropHistory.size >= 20) {
            cropHistory.removeAt(0)
        }
        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        cropHistory.add(bitmap.copy(config, true))
        cropRedoHistory.clear()
    }

    fun undoCrop() {
        if (cropHistory.isNotEmpty()) {
            val current = workingBitmap.value
            if (current != null) {
                val config = current.config ?: Bitmap.Config.ARGB_8888
                cropRedoHistory.add(current.copy(config, true))
            }
            val last = cropHistory.removeAt(cropHistory.lastIndex)
            workingBitmap.value = last
        }
    }

    fun redoCrop() {
        if (cropRedoHistory.isNotEmpty()) {
            val current = workingBitmap.value
            if (current != null) {
                val config = current.config ?: Bitmap.Config.ARGB_8888
                cropHistory.add(current.copy(config, true))
            }
            val next = cropRedoHistory.removeAt(cropRedoHistory.lastIndex)
            workingBitmap.value = next
        }
    }

    fun applyCrop(
        cropLeft: Float,
        cropTop: Float,
        cropRight: Float,
        cropBottom: Float,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val bounds = imageRenderedBounds
        if (bounds.width == 0f || bounds.height == 0f) {
            android.util.Log.w("EditorVM", "applyCrop called before bounds were set — skipping")
            return
        }
        val source = workingBitmap.value ?: return
        
        val cropX = (cropLeft * source.width).roundToInt().coerceIn(0, source.width)
        val cropY = (cropTop * source.height).roundToInt().coerceIn(0, source.height)
        val cropW = ((cropRight - cropLeft) * source.width).roundToInt()
        val cropH = ((cropBottom - cropTop) * source.height).roundToInt()
        
        if (cropW <= 0 || cropH <= 0) {
            onFailure("Select a crop area first")
            return
        }
        
        val finalW = cropW.coerceAtMost(source.width - cropX)
        val finalH = cropH.coerceAtMost(source.height - cropY)
        
        if (finalW <= 0 || finalH <= 0) {
            onFailure("Select a crop area first")
            return
        }
        
        val flatBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        overlayBitmap.value?.let { overlay ->
            val androidOverlay = overlay.asAndroidBitmap()
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            if (androidOverlay.width == flatBitmap.width && androidOverlay.height == flatBitmap.height) {
                Canvas(flatBitmap).drawBitmap(androidOverlay, 0f, 0f, paint)
            } else {
                val scaleMatrix = Matrix().apply {
                    postScale(
                        flatBitmap.width.toFloat() / androidOverlay.width.coerceAtLeast(1),
                        flatBitmap.height.toFloat() / androidOverlay.height.coerceAtLeast(1)
                    )
                }
                Canvas(flatBitmap).drawBitmap(androidOverlay, scaleMatrix, paint)
            }
        }

        saveCropSnapshot(flatBitmap)

        val cropped = Bitmap.createBitmap(flatBitmap, cropX, cropY, finalW, finalH)
        workingBitmap.value = cropped
        val blankOverlay = ImageBitmap(cropped.width, cropped.height)
        overlayBitmap.value = blankOverlay
        overlayCanvas = androidx.compose.ui.graphics.Canvas(blankOverlay)
        undoStack.clear()
        redoStack.clear()
        _cropRect.value = null
        onSuccess()
    }

    private fun bakeBitmap(item: MediaItem, edit: EditState): Bitmap {
        val baseBitmap = workingBitmap.value ?: repo.decodeSoftware(item.uri)
        
        // Apply rotation to the baseBitmap
        val rotatedBitmap = if (edit.rotationDeg % 360f != 0f) {
            val matrix = Matrix().apply { postRotate(edit.rotationDeg) }
            Bitmap.createBitmap(baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true)
        } else {
            baseBitmap
        }

        // Apply filters/color matrix
        val finalBitmap = Bitmap.createBitmap(rotatedBitmap.width, rotatedBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(EditMatrix.combined(edit).array)
        }
        canvas.drawBitmap(rotatedBitmap, 0f, 0f, paint)

        val sharpenedBitmap = if (edit.sharpness > 0f) {
            applySharpness(finalBitmap, edit.sharpness)
        } else {
            finalBitmap
        }
        val adjustedBitmap = if (edit.vignette > 0f) {
            applyVignette(sharpenedBitmap, edit.vignette)
        } else {
            sharpenedBitmap
        }

        // Flatten doodle overlay
        val overlay = overlayBitmap.value
        if (overlay != null) {
            val androidOverlay = overlay.asAndroidBitmap()
            val scaleMatrix = Matrix().apply {
                val scaleX = adjustedBitmap.width.toFloat() / androidOverlay.width
                val scaleY = adjustedBitmap.height.toFloat() / androidOverlay.height
                postScale(scaleX, scaleY)
            }
            val drawPaint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            Canvas(adjustedBitmap).drawBitmap(androidOverlay, scaleMatrix, drawPaint)
        }

        // Recycle rotatedBitmap if it was a newly created copy and not the baseBitmap
        if (rotatedBitmap != baseBitmap) {
            rotatedBitmap.recycle()
        }

        if (sharpenedBitmap != finalBitmap) {
            finalBitmap.recycle()
        }
        if (adjustedBitmap != sharpenedBitmap) {
            sharpenedBitmap.recycle()
        }

        return adjustedBitmap
    }

    fun previewSharpness(source: Bitmap, amount: Float): Bitmap {
        val maxPreviewDim = 1400
        val longest = maxOf(source.width, source.height)
        val previewSource = if (longest > maxPreviewDim) {
            val scale = maxPreviewDim.toFloat() / longest
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).roundToInt().coerceAtLeast(1),
                (source.height * scale).roundToInt().coerceAtLeast(1),
                true,
            )
        } else {
            source
        }
        return applySharpness(previewSource, amount).also {
            if (previewSource != source) previewSource.recycle()
        }
    }

    private fun applySharpness(source: Bitmap, amount: Float): Bitmap {
        val width = source.width
        val height = source.height
        if (width < 3 || height < 3) return source.copy(Bitmap.Config.ARGB_8888, false)

        val strength = amount.coerceIn(0f, 1f) * 1.35f
        val center = 1f + strength * 4f
        val pixels = IntArray(width * height)
        val out = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        pixels.copyInto(out)

        fun channel(value: Float): Int = value.roundToInt().coerceIn(0, 255)

        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val i = row + x
                val c = pixels[i]
                val l = pixels[i - 1]
                val r = pixels[i + 1]
                val t = pixels[i - width]
                val b = pixels[i + width]

                val a = c ushr 24
                val red = channel(((c shr 16) and 0xff) * center - (((l shr 16) and 0xff) + ((r shr 16) and 0xff) + ((t shr 16) and 0xff) + ((b shr 16) and 0xff)) * strength)
                val green = channel(((c shr 8) and 0xff) * center - (((l shr 8) and 0xff) + ((r shr 8) and 0xff) + ((t shr 8) and 0xff) + ((b shr 8) and 0xff)) * strength)
                val blue = channel((c and 0xff) * center - ((l and 0xff) + (r and 0xff) + (t and 0xff) + (b and 0xff)) * strength)
                out[i] = (a shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(out, 0, width, 0, 0, width, height)
        }
    }

    private fun applyVignette(source: Bitmap, amount: Float): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val alpha = (amount.coerceIn(0f, 1f) * 184f).roundToInt().coerceIn(0, 255)
        val radius = maxOf(out.width, out.height) * 0.74f
        val shader = RadialGradient(
            out.width / 2f,
            out.height / 2f,
            radius,
            intArrayOf(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb(alpha, 0, 0, 0),
            ),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        Canvas(out).drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), paint)
        return out
    }

    /** Bake edits at full resolution and save as a new MediaStore image (no consent). */
    fun saveCopy(app: AppViewModel, item: MediaItem, edit: EditState, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            val bmp = bakeBitmap(item, edit)
            repo.saveCopy(bmp, item.displayName)
            bmp.recycle()
            app.showSnackbar.tryEmit("Saved")
            _saving.value = false
            onDone()
        }
    }

    /** Bake edits then request write consent to overwrite the original (handled by AppViewModel). */
    fun overwrite(app: AppViewModel, item: MediaItem, edit: EditState, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            val bmp = bakeBitmap(item, edit)
            app.requestOverwrite(item.uri, bmp) // bitmap held by AppViewModel until consent resolves
            _saving.value = false
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory { container -> EditorViewModel(container) }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
