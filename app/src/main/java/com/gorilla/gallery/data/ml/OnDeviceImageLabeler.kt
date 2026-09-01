package com.gorilla.gallery.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OnDeviceImageLabeler(
    private val context: Context,
) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0f)
            .build()
    )

    suspend fun label(uri: Uri): List<String> {
        val bitmap = decodeSampledBitmap(context, uri, MAX_LABEL_BITMAP_SIZE) ?: return emptyList()
        return try {
            if (bitmap.width < MIN_LABEL_BITMAP_SIZE || bitmap.height < MIN_LABEL_BITMAP_SIZE) {
                return emptyList()
            }
            val image = InputImage.fromBitmap(bitmap, 0)
            val labels = labeler.process(image).await()
            labelsToTags(labels)
        } finally {
            bitmap.recycle()
        }
    }

    private fun labelsToTags(labels: List<com.google.mlkit.vision.label.ImageLabel>): List<String> {
        return labels.asSequence()
            .filter { it.confidence >= MIN_CONFIDENCE }
            .sortedByDescending { it.confidence }
            .map { it.text.lowercase() }
            .distinct()
            .toList()
    }

    companion object {
        private const val MIN_LABEL_BITMAP_SIZE = 32
        private const val MAX_LABEL_BITMAP_SIZE = 224
        private const val MIN_CONFIDENCE = 0.90f
    }
}

suspend fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    runCatching {
        resolver.loadThumbnail(uri, Size(maxDimension, maxDimension), null)
    }.getOrNull()?.let { return@withContext it }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    } ?: return@withContext null

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    val decoded = resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOptions)
    } ?: return@withContext null

    if (decoded.width <= maxDimension && decoded.height <= maxDimension) {
        decoded
    } else {
        val scale = maxDimension.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
        val targetWidth = (decoded.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (decoded.height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true).also {
            if (it != decoded) decoded.recycle()
        }
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sample = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / sample >= maxDimension || halfHeight / sample >= maxDimension) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
