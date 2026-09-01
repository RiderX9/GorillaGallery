package com.gorilla.gallery.data.repo

import com.gorilla.gallery.data.db.AppDatabase

import android.content.Context
import com.gorilla.gallery.data.ml.ClipModelRunner

class ImageLabelRepository(context: Context, database: AppDatabase) {
    private val dao = database.imageLabelDao()
    private val embeddingDao = database.imageEmbeddingDao()
    private val clipRunner = ClipModelRunner(context)

    suspend fun getTags(imagePath: String): List<String> = dao.getTags(imagePath)

    suspend fun searchImagePaths(query: String): Set<String> {
        val clean = query.trim()
        if (clean.isBlank()) return emptySet()

        val textTerms = buildSet {
            add(clean.lowercase())
            singularize(clean.lowercase())?.let(::add)
        }
        return textTerms.flatMap { term -> dao.searchByTag(term) }.map { it.imagePath }.toSet()
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return (dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))).toFloat()
    }

    private fun singularize(term: String): String? =
        when {
            term.length > 3 && term.endsWith("ies") -> term.dropLast(3) + "y"
            term.length > 3 && term.endsWith("ses") -> term.dropLast(2)
            term.length > 3 && term.endsWith("s") && !term.endsWith("ss") -> term.dropLast(1)
            else -> null
        }
}
