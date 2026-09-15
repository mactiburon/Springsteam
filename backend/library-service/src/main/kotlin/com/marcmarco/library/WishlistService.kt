package com.marcmarco.library

import com.marcmarco.library.dto.WishlistAddRequest
import com.marcmarco.shared.error.BadRequestException
import com.marcmarco.shared.error.ConflictException
import com.marcmarco.shared.error.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

enum class WishlistSort {
    ADDED, NAME, GENRE, DEVELOPER, PUBLISHER, RELEASE_DATE;

    companion object {
        fun parse(value: String): WishlistSort =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw BadRequestException(
                    "sort inválido: '$value'. Valores permitidos: ${entries.joinToString { it.name.lowercase() }}",
                )
    }
}

enum class WishlistOrder {
    ASC, DESC;

    companion object {
        fun parse(value: String): WishlistOrder =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw BadRequestException(
                    "order inválido: '$value'. Valores permitidos: ${entries.joinToString { it.name.lowercase() }}",
                )
    }
}

data class WishlistFilters(
    val name: String? = null,
    val genre: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val inLibrary: Boolean? = null,
    val sort: WishlistSort = WishlistSort.ADDED,
    val order: WishlistOrder? = null,
)

/**
 * Wishlist del usuario: añadir, buscar/filtrar/ordenar y eliminar. Todo se
 * filtra por `userId` del JWT; un usuario solo ve y toca su propia wishlist.
 */
@Service
class WishlistService(
    private val wishlistRepository: WishlistRepository,
    private val gameRepository: GameRepository,
) {

    @Transactional
    fun add(userId: Long, request: WishlistAddRequest): WishlistEntry {
        val gameId = request.gameId
            ?: throw BadRequestException("El campo gameId es obligatorio")
        val game = gameRepository.findById(gameId)
            .orElseThrow { NotFoundException("Juego con id $gameId no encontrado") }

        if (wishlistRepository.existsByUserIdAndGameId(userId, gameId)) {
            throw ConflictException("El juego '${game.name}' ya está en la wishlist del usuario $userId")
        }

        return wishlistRepository.save(WishlistEntry(userId = userId, game = game))
    }

    @Transactional(readOnly = true)
    fun search(userId: Long, filters: WishlistFilters): List<WishlistEntry> {
        val base = wishlistRepository.searchWishlist(
            userId = userId,
            name = filters.name ?: "",
            genre = filters.genre ?: "",
            developer = filters.developer ?: "",
            publisher = filters.publisher ?: "",
            libraryScope = when {
                filters.inLibrary == null -> "ALL"
                filters.inLibrary -> "OWNED"
                else -> "NOT_OWNED"
            },
        )
        return sort(base, filters.sort, filters.order)
    }

    @Transactional
    fun remove(userId: Long, gameId: Long) {
        val entry = wishlistRepository.findByUserIdAndGameId(userId, gameId)
            ?: throw NotFoundException("El usuario $userId no tiene el juego $gameId en su wishlist")
        wishlistRepository.delete(entry)
    }

    private fun sort(entries: List<WishlistEntry>, sort: WishlistSort, order: WishlistOrder?): List<WishlistEntry> {
        val effectiveOrder = order ?: if (sort == WishlistSort.ADDED) WishlistOrder.DESC else WishlistOrder.ASC
        return when (sort) {
            WishlistSort.ADDED ->
                if (effectiveOrder == WishlistOrder.ASC) entries.sortedBy { it.addedAt } else entries
            WishlistSort.NAME -> entries.sortedWith(comparing { it.game?.name?.lowercase() })
            WishlistSort.GENRE -> entries.sortedWith(comparing { it.game?.genre?.lowercase() })
            WishlistSort.DEVELOPER -> entries.sortedWith(comparing { it.game?.developer?.lowercase() })
            WishlistSort.PUBLISHER -> entries.sortedWith(comparing { it.game?.publisher?.lowercase() })
            WishlistSort.RELEASE_DATE -> entries.sortedWith(comparing { it.game?.releaseDate })
        }.let { if (sort != WishlistSort.ADDED && effectiveOrder == WishlistOrder.DESC) it.reversed() else it }
    }

    private fun <T : Comparable<T>> comparing(getter: (WishlistEntry) -> T?): Comparator<WishlistEntry> =
        Comparator { a, b ->
            val x = getter(a)
            val y = getter(b)
            when {
                x == null && y == null -> 0
                x == null -> 1
                y == null -> -1
                else -> x.compareTo(y)
            }
        }
}