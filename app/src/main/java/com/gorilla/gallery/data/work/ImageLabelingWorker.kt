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
import com.gorilla.gallery.data.db.FaceEmbeddingEntry
import com.gorilla.gallery.data.model.MediaType
import com.gorilla.gallery.data.ml.FaceClusterer
import com.gorilla.gallery.data.ml.FaceEmbeddingExtractor
import com.gorilla.gallery.data.ml.OnDeviceImageLabeler
import com.gorilla.gallery.data.ml.OnDeviceSelfieDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.gorilla.gallery.data.ml.decodeSampledBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImageLabelingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.get(applicationContext)
        val labelDao = database.imageLabelDao()
        val faceDao = database.faceIndexDao()
        val embeddingDao = database.faceEmbeddingDao()
        labelDao.deleteEmptyLabels()
        val labelIndexed = labelDao.indexedImagePaths()
        val faceIndexed = faceDao.indexedImagePaths()
        val candidates = queryImageUris(database, labelIndexed, faceIndexed, MAX_IMAGES_PER_RUN)
        Log.i(TAG, "Labeling pass found ${candidates.size} candidates; labels=${labelIndexed.size} faces=${faceIndexed.size} already indexed")
        if (candidates.isEmpty()) {
            // Still attempt re-embed/re-cluster even if no new labeling candidates
            runCatching { extractAndClusterEmbeddings(database, embeddingDao) }
            return@withContext Result.success()
        }

        val labeler = OnDeviceImageLabeler(applicationContext)
        val selfieDetector = OnDeviceSelfieDetector(applicationContext)
        var processed = 0
        var facesProcessed = 0
        var empty = 0
        var failed = 0

        for (uri in candidates) {
            coroutineContext.ensureActive()
            val imagePath = uri.toString()
            runCatching {
                if (imagePath !in labelIndexed) {
                    val tags = labeler.label(uri)
                    if (tags.isNotEmpty()) {
                        labelDao.upsert(imagePath, tags)
                        processed++
                    } else {
                        empty++
                    }
                }
                if (imagePath !in faceIndexed) {
                    val result = selfieDetector.detect(uri)
                    faceDao.upsert(imagePath, result)
                    facesProcessed++
                }
            }.onFailure { error ->
                failed++
                Log.w(TAG, "Indexing failed for $imagePath", error)
            }
        }

        // Phase 2: Extract face embeddings for new face images
        runCatching {
            extractAndClusterEmbeddings(database, embeddingDao)
        }.onFailure { error ->
            Log.w(TAG, "Embedding extraction failed", error)
        }

        Log.i(TAG, "Labeling pass saved labels=$processed faces=$facesProcessed empty=$empty failed=$failed")
        if (candidates.size >= MAX_IMAGES_PER_RUN) Result.retry() else Result.success()
    }

    /**
     * Extract embeddings for any face images that don't have embeddings yet,
     * then re-cluster all embeddings.
     */
    private suspend fun extractAndClusterEmbeddings(
        database: AppDatabase,
        embeddingDao: com.gorilla.gallery.data.db.FaceEmbeddingDao,
    ) {
        val faceDao = database.faceIndexDao()

        // Check if we need to force re-embed (after alignment fix)
        val prefs = applicationContext.getSharedPreferences("face_prefs", Context.MODE_PRIVATE)
        val currentVersion = prefs.getInt("embed_version", 0)
        if (currentVersion < EMBED_VERSION) {
            embeddingDao.deleteAll()
            prefs.edit().putInt("embed_version", EMBED_VERSION).apply()
            Log.i(TAG, "Cleared embeddings for re-embed (version upgrade)")
        }

        // Get all face-indexed image paths
        val facePaths = faceDao.peopleImagePaths()
        if (facePaths.isEmpty()) return

        // Get already-embedded paths
        val existingEmbeddings = embeddingDao.allEmbeddings()
        val embeddedPaths = existingEmbeddings.map { it.imagePath }.toSet()

        // Find paths that need embedding
        val toEmbed = facePaths.filter { it !in embeddedPaths }
        if (toEmbed.isEmpty() && existingEmbeddings.isNotEmpty()) {
            // All already embedded - just recluster
            reclusterAll(embeddingDao)
            return
        }
        if (toEmbed.isEmpty()) return

        val extractor = FaceEmbeddingExtractor(applicationContext)
        val faceDetector = com.google.mlkit.vision.face.FaceDetection.getClient(
            com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.08f)
                .build()
        )

        val newEntries = mutableListOf<FaceEmbeddingEntry>()
        var extracted = 0

        for (path in toEmbed) {
            coroutineContext.ensureActive()
            runCatching {
                val uri = Uri.parse(path)
                val bitmap = decodeSampledBitmap(applicationContext, uri, 640) ?: return@runCatching
                try {
                    val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                    val faces = faceDetector.process(image).await()
                    if (faces.isNotEmpty()) {
                        val embeddings = extractor.extractAll(bitmap, faces)
                        for ((idx, emb) in embeddings) {
                            newEntries.add(
                                FaceEmbeddingEntry(
                                    imagePath = path,
                                    faceIndex = idx,
                                    embedding = emb,
                                ),
                            )
                        }
                        extracted++
                    }
                } finally {
                    bitmap.recycle()
                }
            }.onFailure { error ->
                Log.w(TAG, "Embedding extraction failed for $path", error)
            }
        }

        extractor.close()
        faceDetector.close()

        if (newEntries.isNotEmpty()) {
            embeddingDao.upsertBatch(newEntries)
            Log.i(TAG, "Extracted embeddings for $extracted images (${newEntries.size} faces)")
        }

        // Re-cluster all embeddings
        reclusterAll(embeddingDao)
    }

    /**
     * Re-cluster all embeddings from scratch using agglomerative clustering.
     */
    private suspend fun reclusterAll(embeddingDao: com.gorilla.gallery.data.db.FaceEmbeddingDao) {
        val allEmbeddings = embeddingDao.allEmbeddings()
        if (allEmbeddings.isEmpty()) return

        val pairs = allEmbeddings.map { "${it.imagePath}::${it.faceIndex}" to it.embedding }
        val clusterMap = FaceClusterer.cluster(pairs, threshold = CLUSTER_THRESHOLD)

        val updates = allEmbeddings.map { entry ->
            val key = "${entry.imagePath}::${entry.faceIndex}"
            val newClusterId = clusterMap[key] ?: -1
            Triple(newClusterId, entry.imagePath, entry.faceIndex)
        }

        embeddingDao.updateClusterIdsBatch(updates)
        Log.i(TAG, "Re-clustered ${allEmbeddings.size} faces into ${clusterMap.values.distinct().size} clusters")
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "offline_image_labeling"
        private const val INITIAL_WORK_NAME = "offline_image_labeling_initial"
        private const val MAX_IMAGES_PER_RUN = 75
        private const val TAG = "ImageLabelingWorker"
        private const val CLUSTER_THRESHOLD = 0.90f
        private const val EMBED_VERSION = 13

        fun enqueue(context: Context) {
            enqueueUnique(context, INITIAL_WORK_NAME, requiresIdle = false, requiresCharging = false, policy = ExistingWorkPolicy.REPLACE)
            enqueueUnique(context, UNIQUE_WORK_NAME, requiresIdle = true)
            ObjectDetectionWorker.enqueue(context)
            TextRecognitionWorker.enqueue(context)
        }

        private fun enqueueUnique(
            context: Context,
            workName: String,
            requiresIdle: Boolean,
            requiresCharging: Boolean = true,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        ) {
            val constraintsBuilder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            if (requiresCharging) {
                constraintsBuilder.setRequiresCharging(true)
            }
            if (requiresIdle) {
                constraintsBuilder.setRequiresDeviceIdle(true)
            }

            val request = OneTimeWorkRequestBuilder<ImageLabelingWorker>()
                .setConstraints(constraintsBuilder.build())
                .build()

            runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(workName, policy, request)
            }
        }
    }
}

private suspend fun queryImageUris(
    database: AppDatabase,
    labelIndexedPaths: Set<String>,
    faceIndexedPaths: Set<String>,
    limit: Int,
): List<Uri> {
    return database.mediaDao()
        .getAll()
        .asSequence()
        .filter { it.type == MediaType.IMAGE.name }
        .map { it.toItem(isFavorite = false).uri }
        .filter {
            val path = it.toString()
            path !in labelIndexedPaths || path !in faceIndexedPaths
        }
        .take(limit)
        .toList()
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
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

private val <T> kotlinx.coroutines.CancellableContinuation<T>.isActive: Boolean
    get() = true
