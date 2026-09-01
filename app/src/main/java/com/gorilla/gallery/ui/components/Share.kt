package com.gorilla.gallery.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gorilla.gallery.data.model.MediaItem
import java.io.File
import java.util.ArrayList

/** Share one or more media items via the system sheet. Secured (file) items use FileProvider. */
fun shareItems(context: Context, items: List<MediaItem>) {
    if (items.isEmpty()) return
    val uris = ArrayList<Uri>()
    items.forEach { item ->
        val uri = if (item.isSecured) {
            runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(item.uri.path!!))
            }.getOrNull()
        } else item.uri
        if (uri != null) uris.add(uri)
    }
    if (uris.isEmpty()) return

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uris[0])
            type = items.first().mimeType.ifBlank { "*/*" }
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "*/*"
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun exportAsZip(context: Context, items: List<MediaItem>, albumName: String) {
    if (items.isEmpty()) return
    android.widget.Toast.makeText(context, "Zipping ${items.size} items...", android.widget.Toast.LENGTH_SHORT).show()
    Thread {
        try {
            val zipFile = File(context.cacheDir, "${albumName.replace(" ", "_")}_export.zip")
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                items.forEach { item ->
                    val uri = item.uri
                    val name = item.displayName
                    zos.putNextEntry(java.util.zip.ZipEntry(name))
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/zip"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Album").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}
