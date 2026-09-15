package com.marcmarco.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.marcmarco.shared.event.EventEnvelope
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jwtEncoder: JwtEncoder

    @Autowired
    lateinit var auditService: AuditService

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private var userIdCounter = 2_000_000L

    private var gameIdCounter = 800_000L

    private fun token(): String = jwtEncoder.testToken(userIdCounter++)

    private fun record(
        topic: String,
        type: String,
        payload: Map<String, Any?>,
    ): AuditEvent {
        val envelope = EventEnvelope(type = type, occurredAt = Instant.parse("2025-06-01T10:00:00Z"), payload = payload)
        auditService.record(topic, envelope)
        return auditEventRepository.findAll().last()
    }

    @Autowired
    lateinit var auditEventRepository: AuditEventRepository

    // ===== GET /api/audit =====

    @Test
    fun `listar eventos devuelve lista de auditresponse`() {
        val tok = token()
        val uid = userIdCounter - 1
        record("user-events", "user.registered", mapOf("userId" to uid, "username" to "t_$suffix"))

        mockMvc.perform(get("/api/audit").header(HttpHeaders.AUTHORIZATION, "Bearer $tok"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].topic").value("user-events"))
            .andExpect(jsonPath("$[0].type").value("user.registered"))
            .andExpect(jsonPath("$[0].userId").value(uid))
            .andExpect(jsonPath("$[0].payload.username").value("t_$suffix"))
    }

    @Test
    fun `filtrar por topic`() {
        val tok = token()
        record("user-events", "user.registered", mapOf("userId" to 1, "username" to "x"))
        record("game-events", "game.created", mapOf("gameId" to 1, "name" to "g"))

        mockMvc.perform(
            get("/api/audit").header(HttpHeaders.AUTHORIZATION, "Bearer $tok").param("topic", "game-events"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].topic").value("game-events"))
    }

    @Test
    fun `filtrar por userId`() {
        val tok = token()
        val uid = userIdCounter - 1
        record("user-events", "user.registered", mapOf("userId" to uid, "username" to "u"))
        record("library-events", "library.added", mapOf("userId" to uid, "gameId" to 1))
        record("library-events", "library.added", mapOf("userId" to 99999, "gameId" to 1))

        mockMvc.perform(
            get("/api/audit").header(HttpHeaders.AUTHORIZATION, "Bearer $tok").param("userId", uid.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `filtrar por gameId`() {
        val tok = token()
        val gid = gameIdCounter++
        record("game-events", "game.created", mapOf("gameId" to gid, "name" to "h"))
        record("library-events", "library.added", mapOf("userId" to 1, "gameId" to gid))
        record("game-events", "game.created", mapOf("gameId" to 99999, "name" to "x"))

        mockMvc.perform(
            get("/api/audit").header(HttpHeaders.AUTHORIZATION, "Bearer $tok").param("gameId", gid.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `limit respeta el tope`() {
        val tok = token()
        for (i in 1..10) record("game-events", "game.updated", mapOf("gameId" to i, "name" to "g$i"))

        mockMvc.perform(
            get("/api/audit").header(HttpHeaders.AUTHORIZATION, "Bearer $tok").param("limit", "4"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(4))
    }

    // ===== GET /api/audit/{id} =====

    @Test
    fun `obtener evento por id`() {
        val tok = token()
        val event = record("game-events", "game.created", mapOf("gameId" to 42, "name" to "Stray_$suffix"))

        mockMvc.perform(get("/api/audit/${event.id}").header(HttpHeaders.AUTHORIZATION, "Bearer $tok"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(event.id))
            .andExpect(jsonPath("$.topic").value("game-events"))
            .andExpect(jsonPath("$.type").value("game.created"))
            .andExpect(jsonPath("$.gameId").value(42))
            .andExpect(jsonPath("$.payload.name").value("Stray_$suffix"))
    }

    @Test
    fun `evento inexistente devuelve 404`() {
        val tok = token()
        mockMvc.perform(get("/api/audit/999999999").header(HttpHeaders.AUTHORIZATION, "Bearer $tok"))
            .andExpect(status().isNotFound)
    }

    // ===== Seguridad =====

    @Test
    fun `sin token devuelve 401`() {
        mockMvc.perform(get("/api/audit"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `con token invalido devuelve 401`() {
        mockMvc.perform(get("/api/audit").header(HttpHeaders.AUTHORIZATION, "Bearer invalido"))
            .andExpect(status().isUnauthorized)
    }
}