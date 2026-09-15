package com.marcmarco.game.dto

import com.marcmarco.game.Game
import java.time.Instant
import java.time.LocalDate

data class GameResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val genre: String?,
    val releaseDate: LocalDate?,
    val developer: String?,
    val publisher: String?,
    val cover: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(game: Game): GameResponse = GameResponse(
            id = requireNotNull(game.id),
            name = game.name,
            description = game.description,
            genre = game.genre,
            releaseDate = game.releaseDate,
            developer = game.developer,
            publisher = game.publisher,
            cover = game.cover,
            createdAt = game.createdAt,
        )
    }
}