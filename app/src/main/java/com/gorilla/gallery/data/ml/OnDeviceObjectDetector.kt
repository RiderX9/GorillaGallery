package com.gorilla.gallery.data.ml

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DetectedObjectResult(
    val label: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect,
)

class OnDeviceObjectDetector(
    private val context: Context,
) {
    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    suspend fun detect(uri: Uri): List<DetectedObjectResult> {
        val bitmap = decodeSampledBitmap(context, uri, MAX_OBJECT_BITMAP_SIZE)
            ?: return emptyList()
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val objects = detector.process(image).await()
            objects.flatMap { obj ->
                obj.labels.map { label ->
                    DetectedObjectResult(
                        label = label.text.lowercase(),
                        confidence = label.confidence,
                        boundingBox = obj.boundingBox,
                    )
                }
            }.filter { it.confidence >= MIN_CONFIDENCE }
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        private const val MAX_OBJECT_BITMAP_SIZE = 640
        private const val MIN_CONFIDENCE = 0.5f
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
