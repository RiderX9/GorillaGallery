package com.gorilla.gallery.data.db

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FaceIndexResult(
    val faceCount: Int,
    val maxFaceRatio: Float,
    val centerOffset: Float,
)

class FaceIndexDao(private val db: AppDatabase) {

    suspend fun upsert(imagePath: String, result: FaceIndexResult) = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        db.openHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO face_index(imagePath, faceCount, maxFaceRatio, centerOffset, indexedAtSec) " +
                "VALUES(?, ?, ?, ?, ?)",
            arrayOf<Any>(
                imagePath,
                result.faceCount,
                result.maxFaceRatio,
                result.centerOffset,
                System.currentTimeMillis() / 1000,
            ),
        )
    }

    suspend fun indexedImagePaths(): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        db.openHelper.readableDatabase.query(SimpleSQLiteQuery("SELECT imagePath FROM face_index")).use { cursor ->
            buildSet {
                val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) add(cursor.getString(pathIndex))
            }
        }
    }

    suspend fun selfieImagePaths(): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery(
                "SELECT imagePath FROM face_index " +
                    "WHERE faceCount > 0 AND maxFaceRatio >= ? AND centerOffset <= ?",
                arrayOf(MIN_SELFIE_FACE_RATIO, MAX_SELFIE_CENTER_OFFSET),
            )
        ).use { cursor ->
            buildSet {
                val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) add(cursor.getString(pathIndex))
            }
        }
    }

    /** All images that have at least one face detected (people). */
    suspend fun peopleImagePaths(): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery("SELECT imagePath FROM face_index WHERE faceCount > 0"),
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
            "CREATE TABLE IF NOT EXISTS face_index(" +
                "imagePath TEXT NOT NULL PRIMARY KEY, " +
                "faceCount INTEGER NOT NULL, " +
                "maxFaceRatio REAL NOT NULL, " +
                "centerOffset REAL NOT NULL, " +
                "indexedAtSec INTEGER NOT NULL" +
                ")"

        private const val MIN_SELFIE_FACE_RATIO = 0.055f
        private const val MAX_SELFIE_CENTER_OFFSET = 0.48f
    }
}
