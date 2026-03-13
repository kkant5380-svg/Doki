package org.dokiteam.doki.ai.domain

import org.dokiteam.doki.favourites.data.FavouritesDao
import org.dokiteam.doki.history.data.HistoryDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaLibraryManager @Inject constructor(
    private val favouritesDao: FavouritesDao,
    private val historyDao: HistoryDao,
) {

    suspend fun scanForDuplicates(): AiCommandResult {
        val duplicates = findDuplicates()
        return if (duplicates.isEmpty()) {
            AiCommandResult(true, "No duplicates found in your library! Everything looks clean ✨", emptyList<DuplicatePair>())
        } else {
            AiCommandResult(
                success = true,
                message = "Found ${duplicates.size} potential duplicate(s) in your library.",
                data = duplicates,
            )
        }
    }

    suspend fun findDuplicates(): List<DuplicatePair> {
        val favourites = favouritesDao.findAll()
        val history = historyDao.findAll(0, 500)

        val favTitles = favourites.map { it.manga.id to it.manga.title }
        val histTitles = history.map { it.manga.id to it.manga.title }

        val duplicates = mutableListOf<DuplicatePair>()

        // Compare favourites against history
        for ((favId, favTitle) in favTitles) {
            for ((histId, histTitle) in histTitles) {
                if (favId == histId) continue // same DB entry
                val sim = similarity(favTitle, histTitle)
                if (sim >= 0.80) {
                    // Only add if not already present
                    val alreadyAdded = duplicates.any {
                        it.mangaId == favId || it.mangaId == histId
                    }
                    if (!alreadyAdded) {
                        duplicates.add(
                            DuplicatePair(
                                mangaId = histId,
                                title = histTitle,
                                source = "history",
                                similarity = sim,
                                matchTitle = favTitle,
                            )
                        )
                    }
                }
            }
        }

        // Compare within favourites
        for (i in favTitles.indices) {
            for (j in i + 1 until favTitles.size) {
                val (id1, title1) = favTitles[i]
                val (id2, title2) = favTitles[j]
                val sim = similarity(title1, title2)
                if (sim >= 0.80) {
                    val alreadyAdded = duplicates.any { it.mangaId == id1 || it.mangaId == id2 }
                    if (!alreadyAdded) {
                        duplicates.add(
                            DuplicatePair(
                                mangaId = id2,
                                title = title2,
                                source = "favourites",
                                similarity = sim,
                                matchTitle = title1,
                            )
                        )
                    }
                }
            }
        }

        return duplicates
    }

    suspend fun removeFromLibrary(mangaId: Long, source: String): AiCommandResult {
        return runCatching {
            when (source) {
                "history" -> historyDao.delete(mangaId)
                "favourites" -> favouritesDao.delete(mangaId)
                else -> {
                    historyDao.delete(mangaId)
                    favouritesDao.delete(mangaId)
                }
            }
            AiCommandResult(true, "Entry removed successfully.")
        }.getOrElse {
            AiCommandResult(false, "Failed to remove entry: ${it.message}")
        }
    }

    suspend fun getLibrarySummary(): AiCommandResult {
        val favCount = favouritesDao.findAll().size
        val histCount = historyDao.getCount()
        val duplicates = findDuplicates()
        val summary = """
            📚 Library Summary:
            • Favourites: $favCount manga
            • History: $histCount manga
            • Potential duplicates: ${duplicates.size}
            
            ${if (duplicates.isNotEmpty()) "Tip: Scan for duplicates to clean up!" else "Library looks clean!"}
        """.trimIndent()
        return AiCommandResult(true, summary)
    }

    private fun similarity(a: String, b: String): Double {
        val na = a.lowercase().trim()
        val nb = b.lowercase().trim()
        if (na == nb) return 1.0
        if (na.isEmpty() || nb.isEmpty()) return 0.0
        val maxLen = maxOf(na.length, nb.length)
        return 1.0 - (levenshtein(na, nb).toDouble() / maxLen)
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
