package com.marcmarco.library

import com.marcmarco.library.dto.WishlistAddRequest
import com.marcmarco.shared.error.BadRequestException
import com.marcmarco.shared.error.ConflictException
import com.marcmarco.shared.error.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

class WishlistServiceTest {

    private val wishlistRepository: WishlistRepository = mock(WishlistRepository::class.java)
    private val gameRepository: GameRepository = mock(GameRepository::class.java)
    private val service = WishlistService(wishlistRepository, gameRepository)

    private fun game(id: Long, name: String, releaseDate: LocalDate? = null) =
        Game(id = id, name = name, releaseDate = releaseDate)

    private fun entry(g: Game, addedAt: Instant = Instant.ofEpochMilli(g.id ?: 0L)) =
        WishlistEntry(id = g.id, userId = 42L, game = g, addedAt = addedAt)

    private fun stubSearch(vararg items: WishlistEntry) {
        given(
            wishlistRepository.searchWishlist(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()),
        ).willReturn(items.toList())
    }

    // ===== Añadir =====

    @Test
    fun `add con juego inexistente en el catalogo lanza NotFound`() {
        given(gameRepository.findById(500L)).willReturn(Optional.empty())

        val error = assertThrows<NotFoundException> { service.add(42L, WishlistAddRequest(gameId = 500L)) }

        assertEquals("Juego con id 500 no encontrado", error.message)
        verify(gameRepository).findById(500L)
    }

    @Test
    fun `add con juego ya en la wishlist lanza Conflict`() {
        given(gameRepository.findById(1L)).willReturn(Optional.of(game(1L, "Celeste")))
        given(wishlistRepository.existsByUserIdAndGameId(42L, 1L)).willReturn(true)

        val error = assertThrows<ConflictException> { service.add(42L, WishlistAddRequest(gameId = 1L)) }

        assertTrue(error.message.orEmpty().contains("Celeste"))
    }

    @Test
    fun `add guarda la entrada cuando no hay duplicado`() {
        given(gameRepository.findById(1L)).willReturn(Optional.of(game(1L, "Celeste")))
        given(wishlistRepository.existsByUserIdAndGameId(42L, 1L)).willReturn(false)
        given(wishlistRepository.save(any(WishlistEntry::class.java))).willAnswer { it.getArgument(0) }

        val saved = service.add(42L, WishlistAddRequest(gameId = 1L))

        assertEquals(42L, saved.userId)
        assertEquals(1L, saved.game?.id)
        verify(wishlistRepository).save(any(WishlistEntry::class.java))
    }

    @Test
    fun `add con gameId null lanza BadRequest`() {
        val error = assertThrows<BadRequestException> { service.add(42L, WishlistAddRequest(gameId = null)) }

        assertEquals("El campo gameId es obligatorio", error.message)
    }

    // ===== Búsqueda: ámbito =====

    @Test
    fun `search sin inLibrary usa ambito ALL`() {
        stubSearch()

        service.search(42L, WishlistFilters())

        verify(wishlistRepository).searchWishlist(42L, "", "", "", "", "ALL")
    }

    @Test
    fun `search con inLibrary true usa ambito OWNED`() {
        stubSearch()

        service.search(42L, WishlistFilters(inLibrary = true))

        verify(wishlistRepository).searchWishlist(42L, "", "", "", "", "OWNED")
    }

    @Test
    fun `search con inLibrary false usa ambito NOT_OWNED`() {
        stubSearch()

        service.search(42L, WishlistFilters(inLibrary = false))

        verify(wishlistRepository).searchWishlist(42L, "", "", "", "", "NOT_OWNED")
    }

    @Test
    fun `search propaga los filtros de texto al repositorio`() {
        stubSearch()

        service.search(
            42L,
            WishlistFilters(name = "HOLLOW", genre = "Metroidvania", developer = "Team", publisher = "Devolver"),
        )

        verify(wishlistRepository).searchWishlist(42L, "HOLLOW", "Metroidvania", "Team", "Devolver", "ALL")
    }

    // ===== Búsqueda: orden =====

    @Test
    fun `search ordena por nombre ascendente por defecto`() {
        stubSearch(entry(game(3L, "Charlie")), entry(game(1L, "Alfa")), entry(game(2L, "Bravo")))

        val result = service.search(42L, WishlistFilters(sort = WishlistSort.NAME))

        assertEquals(listOf("Alfa", "Bravo", "Charlie"), result.map { it.game?.name })
    }

    @Test
    fun `search ordena por nombre descendente`() {
        stubSearch(entry(game(3L, "Charlie")), entry(game(1L, "Alfa")), entry(game(2L, "Bravo")))

        val result = service.search(42L, WishlistFilters(sort = WishlistSort.NAME, order = WishlistOrder.DESC))

        assertEquals(listOf("Charlie", "Bravo", "Alfa"), result.map { it.game?.name })
    }

    @Test
    fun `search ordena por fecha de lanzamiento ascendente por defecto`() {
        val older = entry(game(1L, "Ancient", releaseDate = LocalDate.of(2010, 1, 1)))
        val newer = entry(game(2L, "Newest", releaseDate = LocalDate.of(2024, 5, 5)))
        stubSearch(older, newer)

        val result = service.search(42L, WishlistFilters(sort = WishlistSort.RELEASE_DATE))

        assertEquals(listOf("Ancient", "Newest"), result.map { it.game?.name })
    }

    @Test
    fun `search ordena por fecha anadida ascendente`() {
        val first = entry(game(1L, "Uno"), addedAt = Instant.ofEpochSecond(100))
        val second = entry(game(2L, "Dos"), addedAt = Instant.ofEpochSecond(200))
        stubSearch(second, first)

        val result = service.search(42L, WishlistFilters(sort = WishlistSort.ADDED, order = WishlistOrder.ASC))

        assertEquals(listOf("Uno", "Dos"), result.map { it.game?.name })
    }

    @Test
    fun `search con sort ADDED y sin order preserva el orden descendente del repositorio`() {
        val later = entry(game(2L, "Dos"), addedAt = Instant.ofEpochSecond(200))
        val earlier = entry(game(1L, "Uno"), addedAt = Instant.ofEpochSecond(100))
        stubSearch(later, earlier)

        val result = service.search(42L, WishlistFilters(sort = WishlistSort.ADDED))

        assertEquals(listOf("Dos", "Uno"), result.map { it.game?.name })
    }

    // ===== Eliminar =====

    @Test
    fun `remove de un juego que no esta en la wishlist lanza NotFound`() {
        given(wishlistRepository.findByUserIdAndGameId(42L, 9L)).willReturn(null)

        assertThrows<NotFoundException> { service.remove(42L, 9L) }
    }

    @Test
    fun `remove borra la entrada de la wishlist`() {
        val item = entry(game(9L, "Algo"))
        given(wishlistRepository.findByUserIdAndGameId(42L, 9L)).willReturn(item)

        service.remove(42L, 9L)

        verify(wishlistRepository).delete(item)
    }
}