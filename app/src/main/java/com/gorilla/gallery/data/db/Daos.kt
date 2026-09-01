package com.gorilla.gallery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaEntity>)

    @Query("SELECT * FROM media ORDER BY dateTakenMs DESC")
    fun observeAll(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media ORDER BY dateTakenMs DESC")
    suspend fun getAll(): List<MediaEntity>

    /** id -> dateModifiedSec, for cheap incremental diffing. */
    @Query("SELECT id, dateModifiedSec FROM media")
    suspend fun getStamps(): List<Stamp>

    @Query("DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE media SET displayName = :displayName WHERE id = :id")
    suspend fun updateDisplayName(id: Long, displayName: String)

    @Query("UPDATE media SET displayName = :displayName, dateTakenMs = :dateTakenMs, dateModifiedSec = :dateModifiedSec WHERE id = :id")
    suspend fun updateMetadata(id: Long, displayName: String, dateTakenMs: Long, dateModifiedSec: Long)

    @Query("SELECT COUNT(*) FROM media")
    suspend fun count(): Int
}

data class Stamp(val id: Long, val dateModifiedSec: Long)

@Dao
interface FavoriteDao {
    @Query("SELECT mediaId FROM favorites")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT mediaId FROM favorites")
    suspend fun getIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun remove(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: Long): Boolean
}

@Dao
interface TrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: TrashEntity)

    @Query("SELECT * FROM trash ORDER BY deletedAtMs DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    @Query("SELECT * FROM trash WHERE id = :id")
    suspend fun getById(id: String): TrashEntity?

    @Query("DELETE FROM trash WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM trash WHERE deletedAtMs < :cutoff")
    suspend fun expired(cutoff: Long): List<TrashEntity>

    @Query("DELETE FROM trash WHERE deletedAtMs < :cutoff")
    suspend fun deleteExpired(cutoff: Long)
}

@Dao
interface SecureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: SecureItemEntity)

    @Query("SELECT * FROM secure_items ORDER BY dateTakenMs DESC")
    fun observeAll(): Flow<List<SecureItemEntity>>

    @Query("SELECT * FROM secure_items WHERE id = :id")
    suspend fun getById(id: String): SecureItemEntity?

    @Query("SELECT COUNT(*) FROM secure_items")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM secure_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
