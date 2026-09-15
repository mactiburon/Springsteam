package com.marcmarco.library.dto

import com.marcmarco.library.WishlistEntry
import java.time.Instant

data class WishlistResponse(
    val userId: Long,
    val gameId: Long,
    val game: GameResponse,
    val addedAt: Instant,
) {
    companion object {
        fun from(entry: WishlistEntry): WishlistResponse = WishlistResponse(
            userId = entry.userId,
            gameId = requireNotNull(entry.game?.id),
            game = GameResponse.from(requireNotNull(entry.game)),
            addedAt = entry.addedAt,
        )
    }
}