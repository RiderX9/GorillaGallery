package com.gorilla.gallery.data.service

import android.app.job.JobInfo
import android.app.job.JobInfo.TriggerContentUri
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.gorilla.gallery.GalleryApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * System JobService triggered by Android's JobScheduler when new media (photos/videos)
 * are added or modified in MediaStore (e.g. downloads in Chrome, camera captures).
 *
 * Runs quietly in the background while the user is still in their browser or camera,
 * indexing the new items into Room DB and pre-caching their thumbnails to disk,
 * so when the user opens GorillaGallery, all new media appears instantly with no blank delay.
 */
class NewMediaJobService : JobService() {

    companion object {
        const val JOB_ID = 2001
        private const val TAG = "NewMediaJobService"
        private val MEDIA_URI = Uri.parse("content://${MediaStore.AUTHORITY}/")

        fun scheduleJob(context: Context) {
            val componentName = ComponentName(context, NewMediaJobService::class.java)
            val photoUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            val builder = JobInfo.Builder(JOB_ID, componentName).apply {
                addTriggerContentUri(TriggerContentUri(photoUri, TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                addTriggerContentUri(TriggerContentUri(videoUri, TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                addTriggerContentUri(TriggerContentUri(MEDIA_URI, 0))

                // Coalesce bursts of downloads (e.g. downloading 6 photos in a web browser)
                setTriggerContentUpdateDelay(500)
                setTriggerContentMaxDelay(2000)
            }

            try {
                val scheduler = context.getSystemService(JobScheduler::class.java)
                scheduler?.schedule(builder.build())
                Log.d(TAG, "Scheduled NewMediaJobService")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule NewMediaJobService", e)
            }
        }

        fun isScheduled(context: Context): Boolean {
            return try {
                val scheduler = context.getSystemService(JobScheduler::class.java)
                scheduler?.allPendingJobs?.any { it.id == JOB_ID } == true
            } catch (e: Exception) {
                false
            }
        }
    }

    private var jobScope: CoroutineScope? = null

    override fun onStartJob(params: JobParameters): Boolean {
        Log.d(TAG, "onStartJob triggered, triggeredContentUris=${params.triggeredContentUris?.size}")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        jobScope = scope

        scope.launch {
            try {
                val app = applicationContext as? GalleryApp
                if (app != null) {
                    val mediaRepo = app.container.mediaRepository
                    val thumbRepo = app.container.thumbnailRepository
                    val settings = app.container.settingsRepository.settings.first()

                    // 1. Incrementally scan and upsert the new items into Room DB
                    val newItems = mediaRepo.incrementalScanItems()
                    Log.d(TAG, "Incremental scan completed in background job, found ${newItems.size} new items")

                    // 2. Pre-cache thumbnails to disk for newly discovered items
                    if (newItems.isNotEmpty()) {
                        val dm = applicationContext.resources.displayMetrics
                        val density = dm.density
                        val spacingPx = (1.5f * 2 * density).toInt()
                        val minDimPx = minOf(dm.widthPixels, dm.heightPixels)
                        val gridCols = settings.gridColumns.coerceAtLeast(1)
                        val sizePx = ((minDimPx / gridCols) - spacingPx).coerceAtLeast(1)
                        val targetSize = if (settings.highQualityThumbnails) sizePx * 2 else sizePx

                        for (item in newItems) {
                            thumbRepo.load(
                                uri = item.uri,
                                mediaId = item.id,
                                sizePx = targetSize,
                                cacheVersion = item.dateModifiedSec,
                                highQuality = settings.highQualityThumbnails,
                                path = item.absolutePath,
                            )
                        }
                        Log.d(TAG, "Pre-cached thumbnails for ${newItems.size} new items with sizePx=$targetSize")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in NewMediaJobService", e)
            } finally {
                // System TriggerContentUri jobs are one-shot; reschedule for subsequent events
                scheduleJob(this@NewMediaJobService)
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        jobScope?.cancel()
        return true
    }
}
