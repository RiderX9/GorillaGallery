@file:android.annotation.SuppressLint("UnsafeOptInUsageError")
package com.gorilla.gallery.data.repo

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.MediaType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File
import java.nio.ByteBuffer

class VideoEditorRepository(private val context: Context) {
    suspend fun saveEditedCopy(
        item: MediaItem,
        startMs: Long,
        endMs: Long,
        removeAudio: Boolean,
        replacementAudioUri: Uri?,
        audioStartMs: Long,
        cropRect: android.graphics.RectF?,
        filterArgb: Int?,
        rotationDegrees: Int = 0,
        overwrite: Boolean = false,
    ): Uri = suspendCancellableCoroutine { cont ->
        val durationMs = item.durationMs.coerceAtLeast(1L)
        val safeStartMs = startMs.coerceIn(0L, durationMs - 1L)
        val safeEndMs = endMs.coerceIn(safeStartMs + 500L, durationMs)
        val tmp = File(context.cacheDir, "video_edit_${System.currentTimeMillis()}.mp4")

        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setUri(item.uri)
            .setClippingConfiguration(
                androidx.media3.common.MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(safeStartMs)
                    .setEndPositionMs(safeEndMs)
                    .build()
            )
            .build()

        val effects = mutableListOf<androidx.media3.common.Effect>()

        if (rotationDegrees != 0) {
            effects.add(androidx.media3.effect.ScaleAndRotateTransformation.Builder().setRotationDegrees(rotationDegrees.toFloat()).build())
        }

        if (cropRect != null) {
            val left = cropRect.left * 2 - 1
            val right = cropRect.right * 2 - 1
            val top = -(cropRect.top * 2 - 1)
            val bottom = -(cropRect.bottom * 2 - 1)
            effects.add(androidx.media3.effect.Crop(left, right, bottom, top))
        }

        if (filterArgb != null) {
            val bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(filterArgb)
            val overlay = object : androidx.media3.effect.BitmapOverlay() {
                override fun getBitmap(presentationTimeUs: Long): android.graphics.Bitmap {
                    return bitmap
                }
            }
            effects.add(androidx.media3.effect.OverlayEffect(com.google.common.collect.ImmutableList.of<androidx.media3.effect.TextureOverlay>(overlay)))
        }

        val editedMediaItem = androidx.media3.transformer.EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(removeAudio)
            .setEffects(androidx.media3.transformer.Effects(emptyList(), effects))
            .build()

        val transformer = androidx.media3.transformer.Transformer.Builder(context)
            .addListener(object : androidx.media3.transformer.Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult) {
                    try {
                        val uri = if (overwrite) {
                            context.contentResolver.openOutputStream(item.uri, "wt")?.use { out ->
                                tmp.inputStream().use { input ->
                                    input.copyTo(out)
                                }
                            } ?: error("Cannot open ${item.uri} for overwrite")
                            item.uri
                        } else {
                            val base = item.displayName.substringBeforeLast('.').ifBlank { "video" }
                            val name = "${base}_edited_${System.currentTimeMillis()}.mp4"
                            MediaIo.insertFromFile(
                                context = context,
                                source = tmp,
                                displayName = name,
                                mimeType = "video/mp4",
                                type = MediaType.VIDEO,
                                relativePath = "Movies/GorillaGallery/",
                                dateTakenMs = item.dateTakenMs,
                            )
                        }
                        tmp.delete()
                        cont.resume(uri)
                    } catch (e: Exception) {
                        tmp.delete()
                        cont.resumeWithException(e)
                    }
                }

                override fun onError(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult, exportException: androidx.media3.transformer.ExportException) {
                    tmp.delete()
                    cont.resumeWithException(exportException)
                }
            })
            .build()
            
        val composition: androidx.media3.transformer.Composition
        if (replacementAudioUri != null) {
            val audioMediaItem = androidx.media3.common.MediaItem.Builder()
                .setUri(replacementAudioUri)
                .setClippingConfiguration(
                    androidx.media3.common.MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(audioStartMs)
                        .build()
                )
                .build()
            val audioEditedItem = androidx.media3.transformer.EditedMediaItem.Builder(audioMediaItem)
                .setRemoveVideo(true)
                .build()
            
            val videoSeq = androidx.media3.transformer.EditedMediaItemSequence.Builder(editedMediaItem).build()
            val audioSeq = androidx.media3.transformer.EditedMediaItemSequence.Builder(audioEditedItem)
                .setIsLooping(true)
                .build()
            
            composition = androidx.media3.transformer.Composition.Builder(listOf(videoSeq, audioSeq))
                .experimentalSetForceAudioTrack(true)
                .build()
        } else {
            composition = androidx.media3.transformer.Composition.Builder(
                listOf(androidx.media3.transformer.EditedMediaItemSequence.Builder(editedMediaItem).build())
            ).build()
        }

        transformer.start(composition, tmp.absolutePath)
        
        cont.invokeOnCancellation {
            transformer.cancel()
            tmp.delete()
        }
    }

    private fun muxTrimmedCopy(
        source: Uri,
        output: File,
        startUs: Long,
        endUs: Long,
        removeAudio: Boolean,
    ) {
        val resolver = context.contentResolver
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var sourceFd: ParcelFileDescriptor? = null

        try {
            sourceFd = resolver.openFileDescriptor(source, "r") ?: error("Cannot open $source")
            extractor.setDataSource(sourceFd.fileDescriptor)
            retriever.setDataSource(sourceFd.fileDescriptor)

            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?.takeIf { it != 0 }
                ?.let { muxer.setOrientationHint(it) }

            val muxerTrackByExtractorTrack = mutableMapOf<Int, Int>()
            for (trackIndex in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(trackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                val isAudio = mime.startsWith("audio/")
                val isVideo = mime.startsWith("video/")
                if (!isVideo && !isAudio) continue
                if (removeAudio && isAudio) continue

                extractor.selectTrack(trackIndex)
                muxerTrackByExtractorTrack[trackIndex] = muxer.addTrack(format)
            }

            check(muxerTrackByExtractorTrack.isNotEmpty()) { "No editable tracks found" }
            muxer.start()

            val bufferSize = maxInputSize(extractor).coerceAtLeast(1 * 1024 * 1024)
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val info = MediaCodec.BufferInfo()

            val lastPtsByTrack = mutableMapOf<Int, Long>()
            var baseTimeUs = -1L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            while (true) {
                val sampleTrack = extractor.sampleTrackIndex
                if (sampleTrack < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0) break

                if (sampleTimeUs > endUs) {
                    extractor.unselectTrack(sampleTrack)
                    extractor.advance()
                    continue
                }

                val muxerTrack = muxerTrackByExtractorTrack[sampleTrack]
                if (muxerTrack == null) {
                    extractor.advance()
                    continue
                }

                if (baseTimeUs == -1L) {
                    baseTimeUs = sampleTimeUs
                }

                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    extractor.unselectTrack(sampleTrack)
                    extractor.advance()
                    continue
                }
                
                var pts = sampleTimeUs - baseTimeUs
                val lastPts = lastPtsByTrack.getOrDefault(sampleTrack, -1L)
                if (pts <= lastPts) {
                    pts = lastPts + 100 // ensure strictly increasing PTS per track
                }
                lastPtsByTrack[sampleTrack] = pts

                info.set(
                    0,
                    sampleSize,
                    pts,
                    if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0,
                )
                muxer.writeSampleData(muxerTrack, buffer, info)
                extractor.advance()
            }
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            extractor.release()
            retriever.release()
            runCatching { sourceFd?.close() }
        }
    }

    private fun maxInputSize(extractor: MediaExtractor): Int {
        var maxSize = 0
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxSize = maxOf(maxSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
            }
        }
        return maxSize
    }
}
