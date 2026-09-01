package com.gorilla.gallery.data.db

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ObjectIndexResult(
    val objects: List<DetectedObject>,
)

data class DetectedObject(
    val label: String,
    val confidence: Float,
)

class ObjectIndexDao(private val db: AppDatabase) {

    suspend fun upsert(imagePath: String, result: ObjectIndexResult) = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        val objectsJson = result.objects.joinToString(";") { "${it.label}:${it.confidence}" }
        db.openHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO object_index(imagePath, objects, indexedAtSec) " +
                "VALUES(?, ?, ?)",
            arrayOf<Any>(
                imagePath,
                objectsJson,
                System.currentTimeMillis() / 1000,
            ),
        )
    }

    suspend fun indexedImagePaths(): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        db.openHelper.readableDatabase.query(SimpleSQLiteQuery("SELECT imagePath FROM object_index")).use { cursor ->
            buildSet {
                val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) add(cursor.getString(pathIndex))
            }
        }
    }

    suspend fun getObjects(imagePath: String): List<DetectedObject> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptyList()
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery("SELECT objects FROM object_index WHERE imagePath = ?", arrayOf(imagePath)),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val objectsStr = cursor.getString(cursor.getColumnIndexOrThrow("objects"))
                objectsStr.split(";").mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        DetectedObject(
                            label = parts[0],
                            confidence = parts[1].toFloatOrNull() ?: 0f,
                        )
                    } else null
                }
            } else emptyList()
        }
    }

    suspend fun searchByObject(query: String): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return@withContext emptySet()
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery("SELECT imagePath, objects FROM object_index WHERE objects LIKE ?", arrayOf("%$clean%")),
        ).use { cursor ->
            buildSet {
                val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                val objectsIndex = cursor.getColumnIndexOrThrow("objects")
                while (cursor.moveToNext()) {
                    val objectsStr = cursor.getString(objectsIndex)
                    val parts = objectsStr.split(";")
                    for (part in parts) {
                        val sub = part.split(":")
                        if (sub.size == 2 && sub[0].lowercase().contains(clean)) {
                            if ((sub[1].toFloatOrNull() ?: 0f) >= 0.90f) {
                                add(cursor.getString(pathIndex))
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ensureTable(): Boolean =
        runCatching {
            db.openHelper.writableDatabase.execSQL(CREATE_TABLE_SQL)
        }.isSuccess

    companion object {
        const val CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS object_index(" +
                "imagePath TEXT NOT NULL PRIMARY KEY, " +
                "objects TEXT NOT NULL, " +
                "indexedAtSec INTEGER NOT NULL" +
                ")"
    }
}
