package com.gorilla.gallery.data.repo

import android.content.ContentResolver
import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ThumbnailRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, Mutex>()

    private val _evictions = MutableSharedFlow<Long>(extraBufferCapacity = 10)
    val evictions: SharedFlow<Long> = _evictions.asSharedFlow()

    fun evict(mediaId: Long) {
        val keys = cache.snapshot().keys.filter { it.startsWith("thumb_${mediaId}_") }
        for (k in keys) cache.remove(k)
        
        // Also wipe from disk
        diskCacheDir.listFiles { file -> file.name.startsWith("thumb_${mediaId}_") }?.forEach { it.delete() }
        
        _evictions.tryEmit(mediaId)
    }

    private val diskCacheDir = java.io.File(context.noBackupFilesDir, "thumbnails").apply { mkdirs() }

    fun getCached(mediaId: Long, sizePx: Int, cacheVersion: Long = 0L, highQuality: Boolean = false): Bitmap? {
        return cache.get(cacheKey(mediaId, sizePx, cacheVersion, highQuality))
    }

    fun getAnyCached(mediaId: Long, cacheVersion: Long = 0L): Bitmap? {
        val prefix = "thumb_${mediaId}_"
        val suffixMatch = "_${cacheVersion}_"
        return cache.snapshot()
            .asSequence()
            .filter { (key, _) -> key.startsWith(prefix) && key.contains(suffixMatch) }
            .maxByOrNull { (_, bitmap) -> bitmap.width * bitmap.height }
            ?.value
    }

    suspend fun load(uri: Uri, mediaId: Long, sizePx: Int, cacheVersion: Long = 0L, highQuality: Boolean = false): Bitmap? {
        val key = cacheKey(mediaId, sizePx, cacheVersion, highQuality)
        cache.get(key)?.let { return it }

        val decodeMutex = lockFor(key)
        return decodeMutex.withLock {
            cache.get(key)?.let { return@withLock it }

            // Check disk cache
            val diskFile = java.io.File(diskCacheDir, key)
            val bitmapFromDisk = withContext(Dispatchers.IO) {
                if (diskFile.exists()) {
                    runCatching {
                        if (highQuality) {
                            java.io.FileInputStream(diskFile).channel.use { channel ->
                                val header = java.nio.ByteBuffer.allocate(13)
                                channel.read(header)
                                header.rewind()
                                val type = header.get().toInt()
                                if (type == 1) {
                                    val w = header.int
                                    val h = header.int
                                    val csId = header.int
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        val cs = if (csId >= 0 && csId < android.graphics.ColorSpace.Named.values().size) {
                                            android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.values()[csId])
                                        } else {
                                            val bt2020 = android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT2020) as android.graphics.ColorSpace.Rgb
                                            android.graphics.ColorSpace.Rgb("BT2020_Gamma29", bt2020.primaries, bt2020.whitePoint, 2.9)
                                        }
                                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGBA_F16, true, cs)
                                        val mappedBuffer = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 13, channel.size() - 13)
                                        bmp.copyPixelsFromBuffer(mappedBuffer)
                                        bmp
                                    } else null
                                } else null
                            }
                        } else {
                            android.graphics.BitmapFactory.decodeFile(diskFile.absolutePath)
                        }
                    }.getOrNull()
                } else null
            }
            if (bitmapFromDisk != null) {
                cache.put(key, bitmapFromDisk)
                return@withLock bitmapFromDisk
            }

            // Generate thumbnail
            val bitmap = withContext(Dispatchers.IO) {
                if (highQuality) {
                    decodeFileThumbnail(uri, sizePx, true) 
                        ?: decodeVideoFileThumbnail(uri, sizePx)
                        ?: runCatching { resolver.loadThumbnail(uri, Size(sizePx, sizePx), null) }.getOrNull()
                } else {
                    runCatching { resolver.loadThumbnail(uri, Size(sizePx, sizePx), null) }
                        .getOrElse { decodeFileThumbnail(uri, sizePx, false) ?: decodeVideoFileThumbnail(uri, sizePx) }
                }
            }
            if (bitmap != null) {
                cache.put(key, bitmap)
                // Save to disk cache
                withContext(Dispatchers.IO) {
                    runCatching {
                        if (highQuality && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            java.io.FileOutputStream(diskFile).channel.use { channel ->
                                val sw = if (bitmap.config != Bitmap.Config.RGBA_F16) {
                                    bitmap.copy(Bitmap.Config.RGBA_F16, false)
                                } else bitmap
                                
                                if (sw != null) {
                                    val header = java.nio.ByteBuffer.allocate(13)
                                    header.put(1.toByte())
                                    header.putInt(sw.width)
                                    header.putInt(sw.height)
                                    header.putInt(sw.colorSpace?.id ?: -1)
                                    header.rewind()
                                    channel.write(header)
                                    
                                    val buffer = java.nio.ByteBuffer.allocateDirect(sw.allocationByteCount)
                                    sw.copyPixelsToBuffer(buffer)
                                    buffer.rewind()
                                    channel.write(buffer)
                                    
                                    if (sw !== bitmap) sw.recycle()
                                }
                            }
                        } else {
                            if (!highQuality) {
                                java.io.FileOutputStream(diskFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                                }
                            }
                        }
                    }
                }
            }
            bitmap
        }.also {
            releaseLock(key, decodeMutex)
        }
    }

    private fun lockFor(key: String): Mutex = inFlight.getOrPut(key) { Mutex() }

    private fun releaseLock(key: String, mutex: Mutex) {
        if (!mutex.isLocked) {
            inFlight.remove(key, mutex)
        }
    }

    private fun cacheKey(mediaId: Long, sizePx: Int, cacheVersion: Long, highQuality: Boolean = false): String =
        "thumb_${mediaId}_${sizePx}_${cacheVersion}_$highQuality"

    private fun decodeFileThumbnail(uri: Uri, sizePx: Int, highQuality: Boolean = false): Bitmap? =
        runCatching {
            val source = if (uri.scheme == ContentResolver.SCHEME_FILE) {
                ImageDecoder.createSource(java.io.File(uri.path ?: return null))
            } else {
                ImageDecoder.createSource(resolver, uri)
            }
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                if (highQuality && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DISPLAY_P3))
                }
                
                val width = info.size.width
                val height = info.size.height
                val longest = maxOf(width, height).coerceAtLeast(1)
                val scale = sizePx.toFloat() / longest
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        }.getOrNull()

    private fun decodeVideoFileThumbnail(uri: Uri, sizePx: Int): Bitmap? {
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                var frame: Bitmap? = null
                
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val w = widthStr?.toIntOrNull() ?: 0
                val h = heightStr?.toIntOrNull() ?: 0
                val scale = if (w > 0 && h > 0) {
                    val longest = maxOf(w, h).coerceAtLeast(1)
                    (sizePx.toFloat() / longest).coerceAtMost(1f)
                } else 1f
                val dstW = (w * scale).toInt().coerceAtLeast(1)
                val dstH = (h * scale).toInt().coerceAtLeast(1)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && w > 0 && h > 0 && scale < 1f) {
                    val params = android.media.MediaMetadataRetriever.BitmapParams()
                    params.preferredConfig = android.graphics.Bitmap.Config.RGBA_F16
                    frame = runCatching { retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, dstW, dstH, params) }.getOrNull()
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val params = android.media.MediaMetadataRetriever.BitmapParams()
                    params.preferredConfig = android.graphics.Bitmap.Config.RGBA_F16
                    frame = runCatching { retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, params) }.getOrNull()
                }
                
                if (frame == null) {
                    frame = retriever.frameAtTime ?: retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                
                if (frame == null) {
                    val bytes = retriever.embeddedPicture
                    if (bytes != null) {
                        frame = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
                
                if (frame != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val transfer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toIntOrNull()
                    val standard = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)?.toIntOrNull()
                    if (transfer == 6 || transfer == 7 || standard == 6) {
                        // 2.7 was slightly too bright, 2.8 was almost perfect, testing 2.9 for deeper shadows.
                        val bt2020 = android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT2020) as android.graphics.ColorSpace.Rgb
                        val cs = android.graphics.ColorSpace.Rgb("BT2020_Gamma29", bt2020.primaries, bt2020.whitePoint, 2.9)
                        if (frame!!.colorSpace?.name != cs.name) {
                            val retagged = Bitmap.createBitmap(frame!!.width, frame!!.height, Bitmap.Config.ARGB_8888, frame!!.hasAlpha(), cs)
                            val sw = if (frame!!.config == Bitmap.Config.HARDWARE) frame!!.copy(Bitmap.Config.ARGB_8888, false) else frame!!
                            if (sw != null) {
                                val buffer = java.nio.ByteBuffer.allocate(sw.allocationByteCount)
                                sw.copyPixelsToBuffer(buffer)
                                buffer.rewind()
                                retagged.copyPixelsFromBuffer(buffer)
                                if (sw !== frame) sw.recycle()
                                frame = retagged
                            }
                        }
                    }
                }
                
                frame?.let { bmp ->
                    if (bmp.width > dstW || bmp.height > dstH) {
                        if (bmp.config == android.graphics.Bitmap.Config.HARDWARE) {
                            bmp 
                        } else {
                            Bitmap.createScaledBitmap(bmp, dstW, dstH, true)
                        }
                    } else bmp
                }
            }
        }.getOrNull()
    }

    private fun cacheSizeKb(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        return (maxMemoryKb * 0.45f).toInt().coerceAtLeast(32 * 1024)
    }
}
