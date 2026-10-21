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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ThumbnailRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, Mutex>()

    private val ioScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val _evictions = MutableSharedFlow<Long>(extraBufferCapacity = 10)
    val evictions: SharedFlow<Long> = _evictions.asSharedFlow()
    private val prefs = context.getSharedPreferences("stale_thumbs", Context.MODE_PRIVATE)
    val staleOsCacheIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>().apply {
        addAll(prefs.getStringSet("stale_ids", emptySet())?.mapNotNull { it.toLongOrNull() } ?: emptySet())
    }

    fun evict(mediaId: Long) {
        val keys = cache.snapshot().keys.filter { it.startsWith("thumb_${mediaId}_") || it.startsWith("thumb_v7_${mediaId}_") }
        for (k in keys) cache.remove(k)
        
        // Also wipe from disk
        diskCacheDir.listFiles { file -> file.name.startsWith("thumb_${mediaId}_") || file.name.startsWith("thumb_v7_${mediaId}_") }?.forEach { it.delete() }
        
        staleOsCacheIds.add(mediaId)
        prefs.edit().putStringSet("stale_ids", staleOsCacheIds.map { it.toString() }.toSet()).apply()
        _evictions.tryEmit(mediaId)
    }

    fun putDirect(mediaId: Long, bitmap: Bitmap, cacheVersion: Long = 0L) {
        val key = cacheKey(mediaId, 512, cacheVersion, false)
        cache.put(key, bitmap)
        val hqKey = cacheKey(mediaId, 1080, cacheVersion, true)
        cache.put(hqKey, bitmap)
        _evictions.tryEmit(mediaId)
    }

    private val diskCacheDir = java.io.File(context.noBackupFilesDir, "thumbnails").apply { mkdirs() }

    fun getCached(mediaId: Long, sizePx: Int, cacheVersion: Long = 0L, highQuality: Boolean = false): Bitmap? {
        val key = cacheKey(mediaId, sizePx, cacheVersion, highQuality)
        val mem = cache.get(key)
        if (mem != null) return mem

        val diskFile = java.io.File(diskCacheDir, key)
        if (diskFile.exists() && diskFile.length() > 0L) {
            val fromDisk = runCatching {
                if (highQuality) null else android.graphics.BitmapFactory.decodeFile(diskFile.absolutePath)
            }.getOrNull()
            if (fromDisk != null) {
                cache.put(key, fromDisk)
                return fromDisk
            }
        }
        return null
    }

    fun getAnyCached(mediaId: Long, cacheVersion: Long = 0L, highQuality: Boolean = false): Bitmap? {
        val prefix = "thumb_${mediaId}_"
        val suffixMatch = "_${cacheVersion}_"
        
        val memFiles = cache.snapshot()
            .asSequence()
            .filter { (key, _) -> key.startsWith(prefix) && key.contains(suffixMatch) }
            
        val memMatch = if (highQuality) {
            memFiles.maxByOrNull {
                val parts = it.key.split("_")
                if (parts.size >= 5) parts[2].toIntOrNull() ?: 0 else 0
            }
        } else {
            memFiles.minByOrNull {
                val parts = it.key.split("_")
                if (parts.size >= 5) parts[2].toIntOrNull() ?: 0 else 0
            }
        }
        
        if (memMatch?.value != null) return memMatch.value

        if (!highQuality) {
            val matchingDisk = diskCacheDir.listFiles { file -> 
                file.name.startsWith(prefix) && file.name.contains(suffixMatch) && file.length() > 0L
            }?.firstOrNull()
            if (matchingDisk != null) {
                val bmp = runCatching { android.graphics.BitmapFactory.decodeFile(matchingDisk.absolutePath) }.getOrNull()
                if (bmp != null) {
                    cache.put(matchingDisk.name, bmp)
                    return bmp
                }
            }
        }

        return null
    }

    suspend fun load(
        uri: Uri,
        mediaId: Long,
        sizePx: Int,
        cacheVersion: Long = 0L,
        highQuality: Boolean = false,
        path: String? = null,
    ): Bitmap? {
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
                val mimeType = runCatching { resolver.getType(uri) }.getOrNull()
                val isSvg = mimeType == "image/svg+xml" || path?.endsWith(".svg", ignoreCase = true) == true || uri.toString().endsWith(".svg", ignoreCase = true)
                if (isSvg) {
                    decodeSvgThumbnail(uri, path ?: "", if (highQuality) sizePx * 2 else sizePx, mediaId)
                } else if (highQuality) {
                    val largeSize = sizePx * 2
                    val isVideo = mimeType?.startsWith("video") == true
                    
                    val osBmp = if (isVideo) {
                        decodeVideoFileThumbnail(uri, largeSize)
                    } else {
                        if (staleOsCacheIds.contains(mediaId)) {
                            decodeFileThumbnail(uri, largeSize, false)
                        } else {
                            runCatching { resolver.loadThumbnail(uri, android.util.Size(largeSize, largeSize), null) }
                                .getOrElse { decodeFileThumbnail(uri, largeSize, false) }
                        }
                    }
                    
                    if (osBmp != null) {
                        if (osBmp.width > sizePx || osBmp.height > sizePx) {
                            val longest = maxOf(osBmp.width, osBmp.height)
                            val scale = sizePx.toFloat() / longest
                            val dstW = (osBmp.width * scale).toInt().coerceAtLeast(1)
                            val dstH = (osBmp.height * scale).toInt().coerceAtLeast(1)
                            Bitmap.createScaledBitmap(osBmp, dstW, dstH, true)
                        } else osBmp
                    } else null
                } else {
                    val mimeType = runCatching { resolver.getType(uri) }.getOrNull()
                    val isVideo = mimeType?.startsWith("video") == true
                    if (isVideo) {
                        decodeVideoFileThumbnail(uri, sizePx)
                    } else {
                        // Direct native file decode first (like Fossify) in ~5ms!
                        decodeDirectFile(path, sizePx)
                            ?: decodeFastThumbnail(uri, sizePx)
                            ?: runCatching { resolver.loadThumbnail(uri, android.util.Size(sizePx, sizePx), null) }.getOrNull()
                            ?: decodeFileThumbnail(uri, sizePx, false)
                    }
                }
            }
            if (bitmap != null) {
                cache.put(key, bitmap)
                // Save to disk cache asynchronously so the decoded bitmap returns immediately to UI
                ioScope.launch {
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

    fun prewarm(items: List<com.gorilla.gallery.data.model.MediaItem>, sizePx: Int, highQuality: Boolean = false) {
        if (items.isEmpty()) return
        ioScope.launch {
            for (item in items) {
                val key = cacheKey(item.id, sizePx, item.dateModifiedSec, highQuality)
                if (cache.get(key) != null) continue
                load(item.uri, item.id, sizePx, item.dateModifiedSec, highQuality, item.absolutePath)
            }
        }
    }

    private fun decodeDirectFile(path: String?, sizePx: Int): Bitmap? {
        if (path.isNullOrEmpty()) return null
        return runCatching {
            val file = java.io.File(path)
            if (!file.exists() || !file.canRead() || file.length() == 0L) return null

            // Read dimensions cheaply in native C++
            val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(path, boundsOpts)
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            val longest = maxOf(origW, origH)
            while (longest / (sampleSize * 2) >= sizePx) {
                sampleSize *= 2
            }

            val decodeOpts = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = android.graphics.BitmapFactory.decodeFile(path, decodeOpts) ?: return null

            // Read EXIF orientation directly from file path in 0.5ms
            val exif = androidx.exifinterface.media.ExifInterface(path)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            var result = decoded
            val matrix = android.graphics.Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (rotated !== decoded) decoded.recycle()
                result = rotated
            }

            val curLongest = maxOf(result.width, result.height)
            if (curLongest > sizePx * 1.3f) {
                val scale = sizePx.toFloat() / curLongest
                val dstW = (result.width * scale).toInt().coerceAtLeast(1)
                val dstH = (result.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(result, dstW, dstH, true)
                if (scaled !== result) result.recycle()
                scaled
            } else {
                result
            }
        }.getOrNull()
    }

    private fun lockFor(key: String): Mutex = inFlight.getOrPut(key) { Mutex() }

    private fun releaseLock(key: String, mutex: Mutex) {
        if (!mutex.isLocked) {
            inFlight.remove(key, mutex)
        }
    }

    private fun cacheKey(mediaId: Long, sizePx: Int, cacheVersion: Long, highQuality: Boolean = false): String =
        "thumb_${mediaId}_${sizePx}_${cacheVersion}_$highQuality"

    private fun decodeFastThumbnail(uri: Uri, sizePx: Int): Bitmap? {
        // Fast path 1: Check embedded EXIF thumbnail (~1ms)
        runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = androidx.exifinterface.media.ExifInterface(stream)
                val thumbBmp = exif.thumbnailBitmap
                if (thumbBmp != null) {
                    val longest = maxOf(thumbBmp.width, thumbBmp.height)
                    val orientation = exif.getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    var result = thumbBmp
                    val matrix = android.graphics.Matrix()
                    when (orientation) {
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    }
                    if (!matrix.isIdentity) {
                        val rotated = Bitmap.createBitmap(thumbBmp, 0, 0, thumbBmp.width, thumbBmp.height, matrix, true)
                        if (rotated !== thumbBmp) thumbBmp.recycle()
                        result = rotated
                    }
                    if (longest > sizePx * 1.5f) {
                        val scale = sizePx.toFloat() / longest
                        val dstW = (result.width * scale).toInt().coerceAtLeast(1)
                        val dstH = (result.height * scale).toInt().coerceAtLeast(1)
                        val scaled = Bitmap.createScaledBitmap(result, dstW, dstH, true)
                        if (scaled !== result) result.recycle()
                        result = scaled
                    }
                    return result
                }
            }
        }

        // Fast path 2: Direct downsample via ImageDecoder.setTargetSize (~10-15ms)
        return decodeFileThumbnail(uri, sizePx, false)
    }

    private fun decodeFileThumbnail(uri: Uri, sizePx: Int, highQuality: Boolean = false): Bitmap? =
        runCatching {
            val source = if (uri.scheme == ContentResolver.SCHEME_FILE) {
                ImageDecoder.createSource(java.io.File(uri.path ?: return null))
            } else {
                ImageDecoder.createSource(resolver, uri)
            }
            
            var targetW = 0
            var targetH = 0
            
            val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                if (highQuality && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DISPLAY_P3))
                }
                
                val width = info.size.width
                val height = info.size.height
                val longest = maxOf(width, height).coerceAtLeast(1)
                val scale = sizePx.toFloat() / longest
                
                if (scale < 1f) {
                    targetW = (width * scale).toInt().coerceAtLeast(1)
                    targetH = (height * scale).toInt().coerceAtLeast(1)
                    decoder.setTargetSize(targetW, targetH)
                }
            }
            
            if (targetW > 0 && targetH > 0 && (decoded.width > targetW * 1.25f || decoded.height > targetH * 1.25f)) {
                val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (scaled !== decoded) {
                    decoded.recycle()
                }
                scaled
            } else {
                decoded
            }
        }.getOrNull()

    private fun decodeVideoFileThumbnail(uri: Uri, sizePx: Int): Bitmap? {
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                } ?: retriever.setDataSource(context, uri)
                var frame: Bitmap? = null
                
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                
                var w = widthStr?.toIntOrNull() ?: 0
                var h = heightStr?.toIntOrNull() ?: 0
                val rotation = rotationStr?.toIntOrNull() ?: 0
                
                if (rotation == 90 || rotation == 270 || rotation == -90 || rotation == -270) {
                    val tmp = w
                    w = h
                    h = tmp
                }
                
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

    val svgDimensionsCache = java.util.concurrent.ConcurrentHashMap<Long, android.util.Size>()

    fun getSvgDimensions(mediaId: Long, uri: Uri): android.util.Size? {
        svgDimensionsCache[mediaId]?.let { return it }
        return runCatching {
            resolver.openInputStream(uri)?.use { s ->
                val svg = com.caverock.androidsvg.SVG.getFromInputStream(s)
                val docW = svg.documentWidth
                val docH = svg.documentHeight
                val vb = svg.documentViewBox
                val srcW = if (docW > 0) docW else vb?.width() ?: 0f
                val srcH = if (docH > 0) docH else vb?.height() ?: 0f
                if (srcW > 0f && srcH > 0f) {
                    val size = android.util.Size(srcW.toInt(), srcH.toInt())
                    svgDimensionsCache[mediaId] = size
                    size
                } else null
            }
        }.getOrNull()
    }

    private fun decodeSvgThumbnail(uri: Uri, path: String, sizePx: Int, mediaId: Long = 0L): Bitmap? {
        return runCatching {
            val stream = runCatching { resolver.openInputStream(uri) }.getOrNull()
                ?: if (path.isNotEmpty() && java.io.File(path).exists()) {
                    runCatching { java.io.FileInputStream(path) }.getOrNull()
                } else null
            stream?.use { s ->
                val svg = com.caverock.androidsvg.SVG.getFromInputStream(s)
                val docW = svg.documentWidth
                val docH = svg.documentHeight
                val vb = svg.documentViewBox
                val srcW = if (docW > 0) docW else vb?.width() ?: 512f
                val srcH = if (docH > 0) docH else vb?.height() ?: 512f
                if (srcW > 0f && srcH > 0f && mediaId != 0L) {
                    svgDimensionsCache[mediaId] = android.util.Size(srcW.toInt(), srcH.toInt())
                }
                val maxDim = maxOf(srcW, srcH).coerceAtLeast(1f)
                val scale = sizePx.toFloat() / maxDim
                val dstW = (srcW * scale).toInt().coerceAtLeast(1)
                val dstH = (srcH * scale).toInt().coerceAtLeast(1)

                if (vb == null && docW > 0 && docH > 0) {
                    svg.setDocumentViewBox(0f, 0f, docW, docH)
                }
                svg.setDocumentWidth("100%")
                svg.setDocumentHeight("100%")

                val bmp = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                svg.renderToCanvas(canvas)
                bmp
            }
        }.getOrNull()
    }

    private fun cacheSizeKb(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        return (maxMemoryKb * 0.45f).toInt().coerceAtLeast(32 * 1024)
    }
}
