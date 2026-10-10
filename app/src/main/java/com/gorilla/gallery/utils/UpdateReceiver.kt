package com.gorilla.gallery.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId != -1L) {
                installApk(context, downloadId)
            }
        } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED == action) {
            val intentOpen = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            intentOpen.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intentOpen)
        }
    }

    private fun installApk(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
        if (apkUri != null) {
            val installIntent = Intent(Intent.ACTION_VIEW)
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.startActivity(installIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
