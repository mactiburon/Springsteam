package com.marcmarco.springbootdemo.library

import com.marcmarco.springbootdemo.common.exception.ConflictException
import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.game.GameRepository
import com.marcmarco.springbootdemo.library.dto.LibraryAddRequest
import com.marcmarco.springbootdemo.library.dto.LibraryUpdateRequest
import com.marcmarco.springbootdemo.user.UserRepository
import org.springframework.stereotype.Service

@Service
class LibraryService(
    private val libraryRepository: LibraryRepository,
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository,
) {

    fun add(userId: Long, request: LibraryAddRequest): LibraryEntry {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("Usuario con id $userId no encontrado") }
        val game = gameRepository.findById(request.gameId)
            .orElseThrow { NotFoundException("Juego con id ${request.gameId} no encontrado") }

        if (libraryRepository.existsByUserIdAndGameId(userId, request.gameId)) {
            throw ConflictException("El juego '${game.name}' ya está en la biblioteca del usuario $userId")
        }

        val entry = LibraryEntry(
            user = user,
            game = game,
            isFavorite = request.isFavorite ?: false,
            hoursPlayed = request.hoursPlayed ?: 0.0,
        )
        return libraryRepository.save(entry)
    }

    fun getEntry(userId: Long, gameId: Long): LibraryEntry =
        libraryRepository.findByUserIdAndGameId(userId, gameId)
            ?: throw NotFoundException("El usuario $userId no tiene el juego $gameId en su biblioteca")

    fun update(userId: Long, gameId: Long, request: LibraryUpdateRequest): LibraryEntry {
        val entry = getEntry(userId, gameId)
        request.isFavorite?.let { entry.isFavorite = it }
        request.hoursPlayed?.let { entry.hoursPlayed = it }
        return libraryRepository.save(entry)
    }

    fun remove(userId: Long, gameId: Long) {
        val entry = getEntry(userId, gameId)
        libraryRepository.delete(entry)
    }

    fun search(userId: Long, name: String? = null, onlyFavorites: Boolean = false): List<LibraryEntry> {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException("Usuario con id $userId no encontrado")
        }
        return libraryRepository.search(
            userId = userId,
            name = name?.takeIf { it.isNotBlank() },
            onlyFavorites = onlyFavorites,
        )
    }
}