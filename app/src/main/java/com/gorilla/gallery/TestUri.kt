package com.gorilla.gallery

import android.content.Context
import android.net.Uri

fun testUri(context: Context) {
    val uri = Uri.parse("content://media/external/images/media/123").buildUpon().appendQueryParameter("cb", "123").build()
    try {
        context.contentResolver.openInputStream(uri)?.close()
    } catch(e: Exception) {
        e.printStackTrace()
    }
}
