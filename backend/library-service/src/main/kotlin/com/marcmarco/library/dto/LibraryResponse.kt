package com.marcmarco.library.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.marcmarco.library.LibraryEntry
import java.time.Instant

data class LibraryResponse(
    val userId: Long,
    val gameId: Long,
    val game: GameResponse,
    @field:JsonProperty("isFavorite")
    val isFavorite: Boolean,
    val hoursPlayed: Double,
    val addedAt: Instant,
) {
    companion object {
        fun from(entry: LibraryEntry): LibraryResponse = LibraryResponse(
            userId = entry.userId,
            gameId = requireNotNull(entry.game.id),
            game = GameResponse.from(entry.game),
            isFavorite = entry.isFavorite,
            hoursPlayed = entry.hoursPlayed,
            addedAt = entry.addedAt,
        )
    }
}