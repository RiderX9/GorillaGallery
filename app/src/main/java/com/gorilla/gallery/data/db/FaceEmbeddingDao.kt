package com.gorilla.gallery.data.db

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceEmbeddingDao(private val db: AppDatabase) {

    suspend fun upsert(
        imagePath: String,
        faceIndex: Int,
        embedding: FloatArray,
        clusterId: Int = -1,
        faceLabel: String? = null,
    ) = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        db.openHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO face_embeddings(imagePath, faceIndex, embedding, clusterId, faceLabel, indexedAtSec) " +
                "VALUES(?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(
                imagePath,
                faceIndex,
                embeddingToBlob(embedding),
                clusterId,
                faceLabel ?: "",
                System.currentTimeMillis() / 1000,
            ),
        )
    }

    suspend fun upsertBatch(entries: List<FaceEmbeddingEntry>) = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        val writable = db.openHelper.writableDatabase
        writable.beginTransaction()
        try {
            for (e in entries) {
                writable.execSQL(
                    "INSERT OR REPLACE INTO face_embeddings(imagePath, faceIndex, embedding, clusterId, faceLabel, indexedAtSec) " +
                        "VALUES(?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        e.imagePath,
                        e.faceIndex,
                        embeddingToBlob(e.embedding),
                        e.clusterId,
                        e.faceLabel ?: "",
                        System.currentTimeMillis() / 1000,
                    ),
                )
            }
            writable.setTransactionSuccessful()
        } finally {
            writable.endTransaction()
        }
    }

    suspend fun updateClusterIdsBatch(updates: List<Triple<Int, String, Int>>) = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        val writable = db.openHelper.writableDatabase
        writable.beginTransaction()
        try {
            for ((clusterId, imagePath, faceIdx) in updates) {
                writable.execSQL(
                    "UPDATE face_embeddings SET clusterId = ? WHERE imagePath = ? AND faceIndex = ?",
                    arrayOf(clusterId, imagePath, faceIdx),
                )
            }
            writable.setTransactionSuccessful()
        } finally {
            writable.endTransaction()
        }
    }

    suspend fun allEmbeddings(): List<FaceEmbeddingEntry> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptyList()
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery("SELECT imagePath, faceIndex, embedding, clusterId, faceLabel FROM face_embeddings"),
        ).use { cursor ->
            buildList {
                val pathIdx = cursor.getColumnIndexOrThrow("imagePath")
                val idxIdx = cursor.getColumnIndexOrThrow("faceIndex")
                val embIdx = cursor.getColumnIndexOrThrow("embedding")
                val clIdx = cursor.getColumnIndexOrThrow("clusterId")
                val lblIdx = cursor.getColumnIndexOrThrow("faceLabel")
                while (cursor.moveToNext()) {
                    val lbl = cursor.getString(lblIdx)
                    add(
                        FaceEmbeddingEntry(
                            imagePath = cursor.getString(pathIdx),
                            faceIndex = cursor.getInt(idxIdx),
                            embedding = blobToEmbedding(cursor.getBlob(embIdx)),
                            clusterId = cursor.getInt(clIdx),
                            faceLabel = if (lbl.isNullOrEmpty()) null else lbl,
                        ),
                    )
                }
            }
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        db.openHelper.writableDatabase.execSQL("DELETE FROM face_embeddings")
    }

    suspend fun distinctClusterIds(): List<Int> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptyList()
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery("SELECT DISTINCT clusterId FROM face_embeddings WHERE clusterId >= 0 ORDER BY clusterId"),
        ).use { cursor ->
            buildList {
                val clIdx = cursor.getColumnIndexOrThrow("clusterId")
                while (cursor.moveToNext()) add(cursor.getInt(clIdx))
            }
        }
    }

    suspend fun imagePathsForCluster(clusterId: Int): Set<String> = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext emptySet()
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery(
                "SELECT DISTINCT imagePath FROM face_embeddings WHERE clusterId = ?",
                arrayOf(clusterId),
            ),
        ).use { cursor ->
            buildSet {
                val pathIdx = cursor.getColumnIndexOrThrow("imagePath")
                while (cursor.moveToNext()) add(cursor.getString(pathIdx))
            }
        }
    }

    suspend fun labelForClusterBatch(clusterIds: List<Int>): Map<Int, String> = withContext(Dispatchers.IO) {
        if (!ensureTable() || clusterIds.isEmpty()) return@withContext emptyMap()
        val placeholders = clusterIds.joinToString(",") { "?" }
        db.openHelper.readableDatabase.query(
            SimpleSQLiteQuery(
                "SELECT clusterId, faceLabel FROM face_embeddings WHERE clusterId IN ($placeholders) AND faceLabel != '' GROUP BY clusterId",
                clusterIds.map { it as Any }.toTypedArray(),
            ),
        ).use { cursor ->
            buildMap {
                val clIdx = cursor.getColumnIndexOrThrow("clusterId")
                val lblIdx = cursor.getColumnIndexOrThrow("faceLabel")
                while (cursor.moveToNext()) {
                    put(cursor.getInt(clIdx), cursor.getString(lblIdx))
                }
            }
        }
    }

    suspend fun updateLabelForCluster(clusterId: Int, label: String) = withContext(Dispatchers.IO) {
        if (!ensureTable()) return@withContext
        db.openHelper.writableDatabase.execSQL(
            "UPDATE face_embeddings SET faceLabel = ? WHERE clusterId = ?",
            arrayOf(label, clusterId),
        )
    }

    private fun ensureTable(): Boolean =
        runCatching {
            db.openHelper.writableDatabase.execSQL(CREATE_TABLE_SQL)
        }.isSuccess

    companion object {
        const val CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS face_embeddings(" +
                "imagePath TEXT NOT NULL, " +
                "faceIndex INTEGER NOT NULL, " +
                "embedding BLOB NOT NULL, " +
                "clusterId INTEGER NOT NULL DEFAULT -1, " +
                "faceLabel TEXT, " +
                "indexedAtSec INTEGER NOT NULL, " +
                "PRIMARY KEY(imagePath, faceIndex)" +
                ")"

        private fun embeddingToBlob(embedding: FloatArray): ByteArray {
            val buffer = ByteBuffer.allocate(embedding.size * 4)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            for (f in embedding) buffer.putFloat(f)
            return buffer.array()
        }

        private fun blobToEmbedding(blob: ByteArray): FloatArray {
            val buffer = ByteBuffer.wrap(blob)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            val embedding = FloatArray(blob.size / 4)
            for (i in embedding.indices) embedding[i] = buffer.float
            return embedding
        }
    }
}

data class FaceEmbeddingEntry(
    val imagePath: String,
    val faceIndex: Int,
    val embedding: FloatArray,
    val clusterId: Int = -1,
    val faceLabel: String? = null,
)
