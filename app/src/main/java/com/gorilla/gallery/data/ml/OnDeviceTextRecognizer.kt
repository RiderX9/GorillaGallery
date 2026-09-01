package com.gorilla.gallery.data.ml

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class RecognizedTextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect?,
)

class OnDeviceTextRecognizer(
    private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(uri: Uri): String {
        val bitmap = decodeSampledBitmap(context, uri, MAX_TEXT_BITMAP_SIZE)
            ?: return ""
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.textBlocks.flatMap { block ->
                block.lines.map { it.text }
            }.joinToString("\n")
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun recognizeTextBlocks(uri: Uri): List<RecognizedTextBlock> {
        val bitmap = decodeSampledBitmap(context, uri, MAX_TEXT_BITMAP_SIZE)
            ?: return emptyList()
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.textBlocks.flatMap { block ->
                block.lines.map { line ->
                    RecognizedTextBlock(
                        text = line.text,
                        boundingBox = line.boundingBox,
                    )
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        private const val MAX_TEXT_BITMAP_SIZE = 1024
    }
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
