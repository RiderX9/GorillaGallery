package com.gorilla.gallery.data.ml

import kotlin.math.sqrt

/**
 * Centroid-based clustering for face embeddings.
 * Completely prevents "chaining" by comparing faces strictly to the cluster's average center.
 */
object FaceClusterer {

    fun cluster(
        embeddings: List<Pair<String, FloatArray>>,
        threshold: Float = DEFAULT_THRESHOLD,
    ): Map<String, Int> {
        if (embeddings.isEmpty()) return emptyMap()

        val clusters = mutableListOf<MutableList<Int>>()
        val sums = mutableListOf<FloatArray>()
        val n = embeddings.size
        
        for (i in 0 until n) {
            val emb = embeddings[i].second
            var bestCluster = -1
            var bestDist = Float.MAX_VALUE
            
            for (c in sums.indices) {
                val cNorm = FloatArray(emb.size)
                var sq = 0f
                for (j in emb.indices) sq += sums[c][j] * sums[c][j]
                val mag = sqrt(sq)
                if (mag > 0f) {
                    for (j in emb.indices) cNorm[j] = sums[c][j] / mag
                } else {
                    for (j in emb.indices) cNorm[j] = 0f
                }
                
                val dist = l2Distance(emb, cNorm)
                if (dist < bestDist) {
                    bestDist = dist
                    bestCluster = c
                }
            }
            
            if (bestCluster != -1 && bestDist <= threshold) {
                clusters[bestCluster].add(i)
                for (j in emb.indices) sums[bestCluster][j] += emb[j]
            } else {
                clusters.add(mutableListOf(i))
                sums.add(emb.copyOf())
            }
        }
        
        val result = mutableMapOf<String, Int>()
        for ((clusterId, clusterIndices) in clusters.withIndex()) {
            // Room cluster IDs typically start from 1 visually, but we map 0-indexed internally
            for (idx in clusterIndices) {
                result[embeddings[idx].first] = clusterId + 1
            }
        }
        return result
    }

    fun l2Distance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    private const val DEFAULT_THRESHOLD = 0.90f
}
