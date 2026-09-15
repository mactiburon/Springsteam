package com.marcmarco.game

import com.marcmarco.game.dto.GameRequest
import com.marcmarco.game.event.GameEventPublisher
import com.marcmarco.shared.error.ConflictException
import com.marcmarco.shared.error.NotFoundException
import com.marcmarco.shared.event.GameEventTypes
import com.marcmarco.shared.event.Topics
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GameService(
    private val gameRepository: GameRepository,
    @Autowired(required = false) private val eventPublisher: GameEventPublisher? = null,
) {

    @Transactional
    fun create(request: GameRequest): Game {
        if (gameRepository.existsByName(request.name)) {
            throw ConflictException("Ya existe un juego llamado '${request.name}'")
        }
        val game = gameRepository.save(
            Game(
                name = request.name,
                description = request.description,
                genre = request.genre,
                releaseDate = request.releaseDate,
                developer = request.developer,
                publisher = request.publisher,
                cover = request.cover,
            ),
        )
        eventPublisher?.publish(
            topic = Topics.GAME_EVENTS,
            type = GameEventTypes.CREATED,
            payload = game.toEventPayload(),
        )
        return game
    }

    fun getGame(id: Long): Game =
        gameRepository.findById(id)
            .orElseThrow { NotFoundException("Juego con id $id no encontrado") }

    @Transactional
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
        val updated = gameRepository.save(game)
        eventPublisher?.publish(
            topic = Topics.GAME_EVENTS,
            type = GameEventTypes.UPDATED,
            payload = updated.toEventPayload(),
        )
        return updated
    }

    @Transactional
    fun delete(id: Long) {
        val game = getGame(id)
        gameRepository.delete(game)
        eventPublisher?.publish(
            topic = Topics.GAME_EVENTS,
            type = GameEventTypes.DELETED,
            payload = mapOf("gameId" to id, "name" to game.name),
        )
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