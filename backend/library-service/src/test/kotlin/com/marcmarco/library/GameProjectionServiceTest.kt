package com.marcmarco.library

import com.marcmarco.library.event.GameProjectionService
import com.marcmarco.shared.event.GameEventTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDate

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GameProjectionServiceTest {

    @Autowired
    lateinit var gameProjectionService: GameProjectionService

    @Autowired
    lateinit var gameRepository: GameRepository

    @Autowired
    lateinit var libraryRepository: LibraryRepository

    @Test
    fun `game created proyecta el juego en la biblioteca`() {
        gameProjectionService.handleGameEvent(
            GameEventTypes.CREATED,
            mapOf(
                "gameId" to 1L,
                "name" to "Half-Life",
                "genre" to "Shooter",
                "releaseDate" to "1998-11-19",
                "developer" to "Valve",
                "publisher" to "Sierra",
                "cover" to "https://img/hl.jpg",
            ),
        )

        val game = gameRepository.findById(1L).orElseThrow()
        assertEquals("Half-Life", game.name)
        assertEquals("Shooter", game.genre)
        assertEquals(LocalDate.parse("1998-11-19"), game.releaseDate)
        assertEquals("Valve", game.developer)
        assertEquals("Sierra", game.publisher)
        assertEquals("https://img/hl.jpg", game.cover)
    }

    @Test
    fun `game updated actualiza la proyeccion existente`() {
        gameProjectionService.handleGameEvent(GameEventTypes.CREATED, mapOf("gameId" to 2L, "name" to "Old"))
        gameProjectionService.handleGameEvent(
            GameEventTypes.UPDATED,
            mapOf("gameId" to 2L, "name" to "Remaster", "genre" to "RPG"),
        )

        val game = gameRepository.findById(2L).orElseThrow()
        assertEquals("Remaster", game.name)
        assertEquals("RPG", game.genre)
    }

    @Test
    fun `game deleted borra el juego y las entradas de biblioteca`() {
        gameProjectionService.handleGameEvent(GameEventTypes.CREATED, mapOf("gameId" to 3L, "name" to "A borrar"))
        val game = gameRepository.findById(3L).orElseThrow()
        libraryRepository.save(LibraryEntry(userId = 7, game = game))

        gameProjectionService.handleGameEvent(GameEventTypes.DELETED, mapOf("gameId" to 3L, "name" to "A borrar"))

        assertFalse(gameRepository.existsById(3L))
        assertTrue(libraryRepository.findByUserIdAndGameId(7, 3L) == null)
    }

    @Test
    fun `tipo de evento desconocido falla con mensaje explicito`() {
        val error = assertThrows<IllegalArgumentException> {
            gameProjectionService.handleGameEvent("game.somethingElse", mapOf("gameId" to 1L))
        }
        assertTrue(error.message.orEmpty().contains("game.somethingElse"))
    }
}