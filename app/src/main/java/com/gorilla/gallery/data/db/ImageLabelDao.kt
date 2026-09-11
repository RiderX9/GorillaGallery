package com.gorilla.gallery.data.db

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImageLabelRow(
    val imagePath: String,
    val tags: String,
)

class ImageLabelDao(private val db: AppDatabase) {

    suspend fun upsert(imagePath: String, tags: List<String>) = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        val tagText = tags
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
        val writable = db.openHelper.writableDatabase
        writable.beginTransaction()
        try {
            writable.execSQL("DELETE FROM image_label_fts WHERE imagePath = ?", arrayOf(imagePath))
            writable.execSQL(
                "INSERT INTO image_label_fts(imagePath, tags) VALUES(?, ?)",
                arrayOf(imagePath, tagText),
            )
            writable.setTransactionSuccessful()
        } finally {
            writable.endTransaction()
        }
    }

    suspend fun indexedImagePaths(): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        val readable = db.openHelper.readableDatabase
        readable.query(SimpleSQLiteQuery("SELECT imagePath FROM image_label_fts")).use { cursor ->
            buildSet {
                val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) add(cursor.getString(pathIndex))
            }
        }
    }

    suspend fun getTags(imagePath: String): List<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptyList()
        val readable = db.openHelper.readableDatabase
        readable.query(
            SimpleSQLiteQuery("SELECT tags FROM image_label_fts WHERE imagePath = ?", arrayOf(imagePath))
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val tagsIndex = cursor.getColumnIndexOrThrow("tags")
                cursor.getString(tagsIndex).split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
        }
    }

    suspend fun deleteEmptyLabels() = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        db.openHelper.writableDatabase.execSQL("DELETE FROM image_label_fts WHERE tags = ''")
    }

    suspend fun searchByTag(tag: String): List<ImageLabelRow> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptyList()
        val match = ftsPhrase(tag)
        if (match.isBlank()) return@withContext emptyList()
        val readable = db.openHelper.readableDatabase
        readable.query(
            SimpleSQLiteQuery(
                "SELECT imagePath, tags FROM image_label_fts WHERE image_label_fts MATCH ?",
                arrayOf(match),
            ),
        ).use { cursor ->
            val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
            val tagsIndex = cursor.getColumnIndexOrThrow("tags")
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ImageLabelRow(
                            imagePath = cursor.getString(pathIndex),
                            tags = cursor.getString(tagsIndex),
                        )
                    )
                }
            }
        }
    }

    /** All image paths whose tags contain any of the given pet-related terms. */
    suspend fun petImagePaths(): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        val readable = db.openHelper.readableDatabase
        val likeClauses = PET_TERMS.joinToString(" OR ") { "tags LIKE '%$it%'" }
        readable.query(
            SimpleSQLiteQuery("SELECT imagePath FROM image_label_fts WHERE $likeClauses"),
        ).use { cursor ->
            buildSet {
                val pathIndex = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) add(cursor.getString(pathIndex))
            }
        }
    }

    private fun ensureTable(): Boolean =
        runCatching {
            db.openHelper.writableDatabase.execSQL(CREATE_FTS_SQL)
        }.isSuccess

    companion object {
        const val CREATE_FTS_SQL =
            "CREATE VIRTUAL TABLE IF NOT EXISTS image_label_fts USING fts4(" +
                "imagePath, " +
                "tags, " +
                "tokenize=unicode61" +
                ")"

        private val PET_TERMS = listOf("cat", "dog", "bird", "fish", "pet", "kitten", "puppy", "rabbit", "hamster", "parrot")

        private fun ftsPhrase(raw: String): String {
            val clean = raw.trim()
            if (clean.isBlank()) return ""
            return "\"${clean.replace("\"", "\"\"")}\""
        }
    }
}
