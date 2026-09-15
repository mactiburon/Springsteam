package com.marcmarco.library.event

import com.marcmarco.library.Game
import com.marcmarco.library.GameRepository
import com.marcmarco.shared.event.GameEventTypes
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

/**
 * Aplica los eventos del catálogo a la proyección local de juegos. Mantiene la
 * tabla `games` de librarydb en sincronía con game-service para poder resolver
 * la biblioteca con el juego anidado sin llamadas síncronas al catálogo.
 */
@Service
class GameProjectionService(
    private val gameRepository: GameRepository,
    private val entityManager: EntityManager,
) {

    @Transactional
    fun handleGameEvent(type: String, payload: Map<String, Any?>) {
        when (type) {
            GameEventTypes.CREATED, GameEventTypes.UPDATED -> upsertGame(payload)
            GameEventTypes.DELETED -> removeGame(payload)
            else -> throw IllegalArgumentException("Tipo de evento de juego desconocido: $type")
        }
    }

    private fun upsertGame(payload: Map<String, Any?>) {
        val gameId = (payload["gameId"] as Number).toLong()
        val name = payload["name"] as String ?: throw IllegalArgumentException("Evento de juego sin 'name' en el payload")
        val existing = gameRepository.findById(gameId).orElse(null)
        if (existing == null) {
            val game = Game(
                id = gameId,
                name = name,
                createdAt = (payload["createdAt"] as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.now(),
            )
            applyOptionalFields(game, payload)
            entityManager.persist(game)
        } else {
            existing.name = name
            applyOptionalFields(existing, payload)
            gameRepository.save(existing)
        }
    }

    private fun applyOptionalFields(game: Game, payload: Map<String, Any?>) {
        game.description = payload["description"] as? String
        game.genre = payload["genre"] as? String
        game.releaseDate = (payload["releaseDate"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        game.developer = payload["developer"] as? String
        game.publisher = payload["publisher"] as? String
        game.cover = payload["cover"] as? String
    }

    private fun removeGame(payload: Map<String, Any?>) {
        val gameId = (payload["gameId"] as Number).toLong()
        gameRepository.findById(gameId).ifPresent { gameRepository.delete(it) }
    }
}