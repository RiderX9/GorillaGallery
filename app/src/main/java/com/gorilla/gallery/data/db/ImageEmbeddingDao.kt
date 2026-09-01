package com.gorilla.gallery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ImageEmbeddingEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBatch(entries: List<ImageEmbeddingEntry>)

    @Query("SELECT * FROM image_embeddings")
    suspend fun allEmbeddings(): List<ImageEmbeddingEntry>

    @Query("DELETE FROM image_embeddings")
    suspend fun deleteAll()
}
