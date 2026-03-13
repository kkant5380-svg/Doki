package org.dokiteam.doki.ai.domain

data class AiCommandResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null,
)

data class DuplicatePair(
    val mangaId: Long,
    val title: String,
    val source: String, // "favourites" or "history"
    val similarity: Double,
    val matchTitle: String,
)
