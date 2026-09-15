package com.marcmarco.library

import com.marcmarco.library.dto.LibraryAddRequest
import com.marcmarco.library.dto.LibraryUpdateRequest
import com.marcmarco.library.event.LibraryEventPublisher
import com.marcmarco.shared.error.ConflictException
import com.marcmarco.shared.error.NotFoundException
import com.marcmarco.shared.event.LibraryEventTypes
import com.marcmarco.shared.event.Topics
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * La identidad del usuario se obtiene del JWT validado por el gateway/auth;
 * por eso aquí el usuario solo existe como `userId`, sin tabla de usuarios.
 */
@Service
class LibraryService(
    private val libraryRepository: LibraryRepository,
    private val gameRepository: GameRepository,
    @Autowired(required = false) private val eventPublisher: LibraryEventPublisher? = null,
) {

    @Transactional
    fun add(userId: Long, request: LibraryAddRequest): LibraryEntry {
        val game = gameRepository.findById(request.gameId)
            .orElseThrow { NotFoundException("Juego con id ${request.gameId} no encontrado") }

        if (libraryRepository.existsByUserIdAndGameId(userId, request.gameId)) {
            throw ConflictException("El juego '${game.name}' ya está en la biblioteca del usuario $userId")
        }

        val entry = libraryRepository.save(
            LibraryEntry(
                userId = userId,
                game = game,
                isFavorite = request.isFavorite ?: false,
                hoursPlayed = request.hoursPlayed ?: 0.0,
                status = request.status ?: LibraryStatus.PENDING,
                lastPlayedAt = request.lastPlayedAt,
            ),
        )
        eventPublisher?.publish(
            topic = Topics.LIBRARY_EVENTS,
            type = LibraryEventTypes.ADDED,
            payload = entry.toEventPayload(),
        )
        return entry
    }

    fun getEntry(userId: Long, gameId: Long): LibraryEntry =
        libraryRepository.findByUserIdAndGameId(userId, gameId)
            ?: throw NotFoundException("El usuario $userId no tiene el juego $gameId en su biblioteca")

    @Transactional
    fun update(userId: Long, gameId: Long, request: LibraryUpdateRequest): LibraryEntry {
        val entry = getEntry(userId, gameId)
        request.isFavorite?.let { entry.isFavorite = it }
        request.hoursPlayed?.let { entry.hoursPlayed = it }
        request.status?.let { entry.status = it }
        request.lastPlayedAt?.let { entry.lastPlayedAt = it }
        val updated = libraryRepository.save(entry)
        eventPublisher?.publish(
            topic = Topics.LIBRARY_EVENTS,
            type = LibraryEventTypes.UPDATED,
            payload = updated.toEventPayload(),
        )
        return updated
    }

    @Transactional
    fun remove(userId: Long, gameId: Long) {
        val entry = getEntry(userId, gameId)
        libraryRepository.delete(entry)
        eventPublisher?.publish(
            topic = Topics.LIBRARY_EVENTS,
            type = LibraryEventTypes.REMOVED,
            payload = mapOf("userId" to userId, "gameId" to gameId),
        )
    }

    fun search(userId: Long, name: String? = null, onlyFavorites: Boolean = false): List<LibraryEntry> =
        libraryRepository.search(
            userId = userId,
            name = name?.takeIf { it.isNotBlank() } ?: "",
            onlyFavorites = onlyFavorites,
        )
}

private fun LibraryEntry.toEventPayload(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "gameId" to (game.id ?: 0L),
    "isFavorite" to isFavorite,
    "hoursPlayed" to hoursPlayed,
    "status" to status.name,
    "lastPlayedAt" to lastPlayedAt?.toString(),
)