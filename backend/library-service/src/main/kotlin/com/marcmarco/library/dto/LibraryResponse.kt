package com.marcmarco.library.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.marcmarco.library.LibraryEntry
import com.marcmarco.library.LibraryStatus
import java.time.Instant

data class LibraryResponse(
    val userId: Long,
    val gameId: Long,
    val game: GameResponse,
    @field:JsonProperty("isFavorite")
    val isFavorite: Boolean,
    val hoursPlayed: Double,
    val status: LibraryStatus,
    val lastPlayedAt: Instant?,
    val categories: List<CategorySummary>,
    val addedAt: Instant,
) {
    companion object {
        fun from(entry: LibraryEntry): LibraryResponse = LibraryResponse(
            userId = entry.userId,
            gameId = requireNotNull(entry.game?.id),
            game = GameResponse.from(requireNotNull(entry.game)),
            isFavorite = entry.isFavorite,
            hoursPlayed = entry.hoursPlayed,
            status = entry.status,
            lastPlayedAt = entry.lastPlayedAt,
            categories = entry.categories.sortedBy { it.name }.map { CategorySummary.from(it) },
            addedAt = entry.addedAt,
        )
    }
}