package com.gorilla.gallery.data.db

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TextIndexResult(
    val text: String,
)

class TextIndexDao(private val db: AppDatabase) {

    suspend fun upsert(imagePath: String, result: TextIndexResult) = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        db.openHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO text_index(imagePath, recognizedText, indexedAtSec) " +
                "VALUES(?, ?, ?)",
            arrayOf<Any>(
                imagePath,
                result.text,
                System.currentTimeMillis() / 1000,
            ),
        )
    }

    suspend fun indexedImagePaths(): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        db.openHelper.readableDatabase.query(SimpleSQLiteQuery("SELECT imagePath FROM text_index")).use { cursor ->
            buildSet {
                val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) add(cursor.getString(pathIndex))
            }
        }
    }

    suspend fun getText(imagePath: String): String = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext ""
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery("SELECT recognizedText FROM text_index WHERE imagePath = ?", arrayOf(imagePath)),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow("recognizedText"))
            } else ""
        }
    }

    suspend fun searchByText(query: String): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return@withContext emptySet()
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery("SELECT imagePath FROM text_index WHERE recognizedText LIKE ?", arrayOf("%$clean%")),
        ).use { cursor ->
            buildSet {
                val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) add(cursor.getString(pathIndex))
            }
        }
    }

    private fun ensureTable(): Boolean =
        runCatching {
            db.openHelper.writableDatabase.execSQL(CREATE_TABLE_SQL)
        }.isSuccess

    companion object {
        const val CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS text_index(" +
                "imagePath TEXT NOT NULL PRIMARY KEY, " +
                "recognizedText TEXT NOT NULL, " +
                "indexedAtSec INTEGER NOT NULL" +
                ")"
    }
}
