package com.marcmarco.audit

import com.marcmarco.shared.event.EventEnvelope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class AuditServiceTest {

    @Autowired
    lateinit var auditService: AuditService

    @Autowired
    lateinit var auditEventRepository: AuditEventRepository

    private fun userPayload(userId: Long = 1L, username: String = "alice") =
        mapOf<String, Any?>("userId" to userId, "username" to username)

    private fun gamePayload(gameId: Long = 10L, name: String = "Celeste") =
        mapOf<String, Any?>("gameId" to gameId, "name" to name)

    private fun libraryPayload(userId: Long = 1L, gameId: Long = 10L) =
        mapOf<String, Any?>("userId" to userId, "gameId" to gameId, "isFavorite" to false, "hoursPlayed" to 5.0)

    private fun record(type: String, topic: String, payload: Map<String, Any?>): AuditEvent {
        val envelope = EventEnvelope(type = type, occurredAt = Instant.parse("2025-06-01T10:00:00Z"), payload = payload)
        auditService.record(topic, envelope)
        return auditEventRepository.findAll().last()
    }

    @Test
    fun `registrar evento de usuario guarda topic type y payload`() {
        val event = record("user.registered", "user-events", userPayload(userId = 42, username = "bob"))
        assertEquals("user-events", event.topic)
        assertEquals("user.registered", event.type)
        assertEquals(42L, event.userId)
        assertEquals(null, event.gameId)
        assertTrue(event.payloadJson.contains("bob"))
    }

    @Test
    fun `registrar evento de juego extrae gameId`() {
        val event = record("game.created", "game-events", gamePayload(gameId = 7, name = "Hades"))
        assertEquals(7L, event.gameId)
        assertEquals(null, event.userId)
        assertTrue(event.payloadJson.contains("Hades"))
    }

    @Test
    fun `registrar evento de biblioteca extrae userId y gameId`() {
        val event = record("library.added", "library-events", libraryPayload(userId = 3, gameId = 5))
        assertEquals(3L, event.userId)
        assertEquals(5L, event.gameId)
    }

    @Test
    fun `search por topic devuelve solo los del topic indicado`() {
        record("user.registered", "user-events", userPayload())
        record("game.created", "game-events", gamePayload())

        val userEvents = auditService.search(topic = "user-events", limit = 100)
        assertTrue(userEvents.all { it.topic == "user-events" })
        assertEquals(1, userEvents.size)
    }

    @Test
    fun `search por userId devuelve solo eventos de ese usuario`() {
        record("user.registered", "user-events", userPayload(userId = 9))
        record("library.added", "library-events", libraryPayload(userId = 9, gameId = 2))
        record("library.added", "library-events", libraryPayload(userId = 7, gameId = 2))

        val events = auditService.search(userId = 9, limit = 100)
        assertEquals(2, events.size)
        assertTrue(events.all { it.userId == 9L })
    }

    @Test
    fun `search por gameId devuelve eventos de juegos y biblioteca`() {
        record("game.created", "game-events", gamePayload(gameId = 4))
        record("library.added", "library-events", libraryPayload(userId = 1, gameId = 4))
        record("game.created", "game-events", gamePayload(gameId = 8))

        val events = auditService.search(gameId = 4, limit = 100)
        assertEquals(2, events.size)
        assertTrue(events.all { it.gameId == 4L })
    }

    @Test
    fun `search limita los resultados`() {
        for (i in 1L..5L) record("game.updated", "game-events", gamePayload(gameId = i))

        val limited = auditService.search(topic = "game-events", limit = 3)
        assertEquals(3, limited.size)
    }

    @Test
    fun `search sin filtros devuelve todo ordenado por occurredAt desc`() {
        record("game.created", "game-events", gamePayload(gameId = 1))
        record("user.registered", "user-events", userPayload(userId = 1))
        record("game.updated", "game-events", gamePayload(gameId = 2))

        val all = auditService.search(limit = 1000)
        assertEquals(3, all.size)
        // all returned in reverse order (most recent first)
        assertTrue(all[0].occurredAt >= all[1].occurredAt)
    }

    @Test
    fun `get retorna evento existente`() {
        val created = record("game.created", "game-events", gamePayload(gameId = 6))
        val found = auditService.get(requireNotNull(created.id))
        assertEquals("game.created", found.type)
    }

    @Test
    fun `get lanza NotFoundException si el id no existe`() {
        assertThrows<com.marcmarco.shared.error.NotFoundException> { auditService.get(999_999L) }
    }
}