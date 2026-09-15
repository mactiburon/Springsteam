package com.marcmarco.springbootdemo.game

import com.marcmarco.springbootdemo.common.exception.ConflictException
import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.game.dto.GameRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GameService(private val gameRepository: GameRepository) {

    fun create(request: GameRequest): Game {
        if (gameRepository.existsByName(request.name)) {
            throw ConflictException("Ya existe un juego llamado '${request.name}'")
        }
        val game = Game(
            name = request.name,
            description = request.description,
            genre = request.genre,
            releaseDate = request.releaseDate,
            developer = request.developer,
            publisher = request.publisher,
            cover = request.cover,
        )
        return gameRepository.save(game)
    }

    fun getGame(id: Long): Game =
        gameRepository.findById(id)
            .orElseThrow { NotFoundException("Juego con id $id no encontrado") }

    fun update(id: Long, request: GameRequest): Game {
        val game = getGame(id)
        if (game.name != request.name && gameRepository.existsByName(request.name)) {
            throw ConflictException("Ya existe un juego llamado '${request.name}'")
        }
        game.name = request.name
        game.description = request.description
        game.genre = request.genre
        game.releaseDate = request.releaseDate
        game.developer = request.developer
        game.publisher = request.publisher
        game.cover = request.cover
        return gameRepository.save(game)
    }

    fun delete(id: Long) {
        val game = getGame(id)
        gameRepository.delete(game)
    }

    fun search(
        name: String? = null,
        genre: String? = null,
        developer: String? = null,
        publisher: String? = null,
        releaseDateFrom: LocalDate? = null,
        releaseDateTo: LocalDate? = null,
    ): List<Game> = gameRepository.search(
        name = name?.takeIf { it.isNotBlank() } ?: "",
        genre = genre?.takeIf { it.isNotBlank() } ?: "",
        developer = developer?.takeIf { it.isNotBlank() } ?: "",
        publisher = publisher?.takeIf { it.isNotBlank() } ?: "",
        releaseDateFrom = releaseDateFrom ?: LocalDate.of(1, 1, 1),
        releaseDateTo = releaseDateTo ?: LocalDate.of(9999, 12, 31),
    )
}