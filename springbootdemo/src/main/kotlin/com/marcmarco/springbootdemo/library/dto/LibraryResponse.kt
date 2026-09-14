package com.marcmarco.springbootdemo.library.dto

import com.marcmarco.springbootdemo.game.dto.GameResponse
import com.marcmarco.springbootdemo.library.LibraryEntry
import java.time.Instant

data class LibraryResponse(
    val userId: Long,
    val gameId: Long,
    val game: GameResponse,
    val isFavorite: Boolean,
    val hoursPlayed: Double,
    val addedAt: Instant,
) {
    companion object {
        fun from(entry: LibraryEntry): LibraryResponse = LibraryResponse(
            userId = requireNotNull(entry.user.id),
            gameId = requireNotNull(entry.game.id),
            game = GameResponse.from(entry.game),
            isFavorite = entry.isFavorite,
            hoursPlayed = entry.hoursPlayed,
            addedAt = entry.addedAt,
        )
    }
}