package com.gorilla.gallery.ui.components

import java.util.Locale
import java.util.concurrent.TimeUnit

/** mm:ss (or h:mm:ss) duration formatting for video badges/seek bars. */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = TimeUnit.SECONDS.toHours(totalSec)
    val m = TimeUnit.SECONDS.toMinutes(totalSec) % 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

/** Human-readable file size. */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unit = 0
    while (size >= 1024 && unit < units.lastIndex) {
        size /= 1024
        unit++
    }
    return String.format(Locale.US, "%.1f %s", size, units[unit])
}

/** "4032 × 3024" resolution, with a megapixel hint. */
fun formatResolution(width: Int, height: Int): String =
    if (width <= 0 || height <= 0) "—" else "$width × $height"

fun megapixels(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "—"
    val mp = (width.toLong() * height) / 1_000_000.0
    return String.format(Locale.US, "%.1f MP", mp)
}
