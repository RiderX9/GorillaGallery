package com.gorilla.gallery.data.work

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gorilla.gallery.data.db.AppDatabase
import com.gorilla.gallery.data.db.TextIndexResult
import com.gorilla.gallery.data.model.MediaType
import com.gorilla.gallery.data.ml.OnDeviceTextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class TextRecognitionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.get(applicationContext)
        val textDao = database.textIndexDao()
        val indexed = textDao.indexedImagePaths()
        val candidates = queryImageUris(database, indexed, MAX_IMAGES_PER_RUN)
        Log.i(TAG, "Text recognition pass found ${candidates.size} candidates; already indexed=${indexed.size}")
        if (candidates.isEmpty()) return@withContext Result.success()

        val recognizer = OnDeviceTextRecognizer(applicationContext)
        var processed = 0
        var empty = 0
        var failed = 0

        for (uri in candidates) {
            coroutineContext.ensureActive()
            val imagePath = uri.toString()
            runCatching {
                val text = recognizer.recognizeText(uri)
                if (text.isNotBlank()) {
                    textDao.upsert(imagePath, TextIndexResult(text = text))
                    processed++
                } else {
                    empty++
                }
            }.onFailure { error ->
                failed++
                Log.w(TAG, "Text recognition failed for $imagePath", error)
            }
        }

        Log.i(TAG, "Text recognition pass saved processed=$processed empty=$empty failed=$failed")
        if (candidates.size >= MAX_IMAGES_PER_RUN) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "offline_text_recognition"
        private const val INITIAL_WORK_NAME = "offline_text_recognition_initial"
        private const val MAX_IMAGES_PER_RUN = 75
        private const val TAG = "TextRecognitionWorker"

        fun enqueue(context: Context) {
            enqueueUnique(context, INITIAL_WORK_NAME, requiresIdle = false)
            enqueueUnique(context, UNIQUE_WORK_NAME, requiresIdle = true)
        }

        private fun enqueueUnique(
            context: Context,
            workName: String,
            requiresIdle: Boolean,
        ) {
            val constraintsBuilder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(true)
            if (requiresIdle) {
                constraintsBuilder.setRequiresDeviceIdle(true)
            }

            val request = OneTimeWorkRequestBuilder<TextRecognitionWorker>()
                .setConstraints(constraintsBuilder.build())
                .build()

            runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)
            }
        }
    }
}

suspend private fun queryImageUris(
    database: AppDatabase,
    indexedPaths: Set<String>,
    limit: Int,
): List<Uri> {
    return database.mediaDao()
        .getAll()
        .asSequence()
        .filter { it.type == MediaType.IMAGE.name }
        .map { it.toItem(isFavorite = false).uri }
        .filter { it.toString() !in indexedPaths }
        .take(limit)
        .toList()
}
