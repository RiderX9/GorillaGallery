package com.gorilla.gallery.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_embeddings")
data class ImageEmbeddingEntry(
    @PrimaryKey val imagePath: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val embedding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageEmbeddingEntry

        if (imagePath != other.imagePath) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imagePath.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
