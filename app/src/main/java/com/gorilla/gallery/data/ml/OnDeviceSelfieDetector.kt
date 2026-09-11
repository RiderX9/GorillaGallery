package com.gorilla.gallery.data.ml

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.gorilla.gallery.data.db.FaceIndexResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OnDeviceSelfieDetector(
    private val context: Context,
) {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(MIN_FACE_SIZE)
            .build()
    )

    suspend fun detect(uri: Uri): FaceIndexResult {
        val bitmap = decodeSampledBitmap(context, uri, MAX_FACE_BITMAP_SIZE)
            ?: return FaceIndexResult(faceCount = 0, maxFaceRatio = 0f, centerOffset = 1f)
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()
            if (faces.isEmpty()) {
                FaceIndexResult(faceCount = 0, maxFaceRatio = 0f, centerOffset = 1f)
            } else {
                val imageArea = bitmap.width.toFloat() * bitmap.height.toFloat()
                val imageCenterX = bitmap.width / 2f
                val imageCenterY = bitmap.height / 2f
                val maxCenterDistance = kotlin.math.hypot(imageCenterX, imageCenterY).coerceAtLeast(1f)
                val largest = faces.maxBy { face ->
                    face.boundingBox.width().coerceAtLeast(0) * face.boundingBox.height().coerceAtLeast(0)
                }
                val box = largest.boundingBox
                val faceArea = box.width().coerceAtLeast(0).toFloat() * box.height().coerceAtLeast(0).toFloat()
                val faceCenterX = box.exactCenterX()
                val faceCenterY = box.exactCenterY()
                FaceIndexResult(
                    faceCount = faces.size,
                    maxFaceRatio = (faceArea / imageArea).coerceIn(0f, 1f),
                    centerOffset = (kotlin.math.hypot(faceCenterX - imageCenterX, faceCenterY - imageCenterY) / maxCenterDistance)
                        .coerceIn(0f, 1f),
                )
            }
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        private const val MAX_FACE_BITMAP_SIZE = 640
        private const val MIN_FACE_SIZE = 0.08f
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
