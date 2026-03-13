package org.dokiteam.doki.ai.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponseCache @Inject constructor() {

    private data class CacheEntry(
        val query: String,
        val response: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val cache = mutableListOf<CacheEntry>()
    private val ttlMs = 24 * 60 * 60 * 1000L // 24 hours
    private val similarityThreshold = 0.80

    fun get(query: String): String? {
        removeExpired()
        val normalizedQuery = query.lowercase().trim()
        return cache
            .filter { similarity(it.query.lowercase().trim(), normalizedQuery) >= similarityThreshold }
            .maxByOrNull { similarity(it.query.lowercase().trim(), normalizedQuery) }
            ?.response
    }

    fun put(query: String, response: String) {
        removeExpired()
        // Replace if exact match exists, otherwise add
        val existing = cache.indexOfFirst { it.query.equals(query, ignoreCase = true) }
        if (existing >= 0) {
            cache[existing] = CacheEntry(query, response)
        } else {
            if (cache.size >= 100) cache.removeAt(0)
            cache.add(CacheEntry(query, response))
        }
    }

    fun clear() = cache.clear()

    fun removeExpired() {
        val cutoff = System.currentTimeMillis() - ttlMs
        cache.removeAll { it.timestamp < cutoff }
    }

    /**
     * Levenshtein distance-based similarity ratio between 0.0 and 1.0
     */
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val maxLen = maxOf(a.length, b.length)
        return 1.0 - (levenshtein(a, b).toDouble() / maxLen)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }
}
