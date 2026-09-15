package com.marcmarco.library

import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LibraryControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jwtEncoder: JwtEncoder

    @Autowired
    lateinit var gameRepository: GameRepository

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private var userIdCounter = 1_000_000L

    private var gameIdCounter = 900_000L

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private data class Session(val userId: Long, val token: String)

    private fun session(): Session {
        val userId = userIdCounter++
        return Session(userId = userId, token = jwtEncoder.testToken(userId))
    }

    private fun createGame(name: String): Long {
        val id = gameIdCounter++
        gameRepository.save(Game(id = id, name = name))
        return id
    }

    private fun addGame(token: String, gameId: Long, isFavorite: Boolean = false, hoursPlayed: Double? = null): String {
        val body = jsonBody(
            mapOf(
                "gameId" to gameId,
                "isFavorite" to isFavorite,
                "hoursPlayed" to hoursPlayed,
            ),
        )
        return mockMvc.perform(
            post("/api/library")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString
    }

    // ===== Añadir =====

    @Test
    fun `anadir juego a la biblioteca devuelve 201 con el juego anidado`() {
        val session = session()
        val gameId = createGame("Almas_$suffix")
        val response = addGame(session.token, gameId, isFavorite = true, hoursPlayed = 12.5)
        val tree = objectMapper.readTree(response)
        org.junit.jupiter.api.Assertions.assertEquals(session.userId, tree.path("userId").asLong())
        org.junit.jupiter.api.Assertions.assertEquals(gameId, tree.path("gameId").asLong())
        org.junit.jupiter.api.Assertions.assertEquals("Almas_$suffix", tree.path("game").path("name").asText())
        org.junit.jupiter.api.Assertions.assertEquals(true, tree.path("isFavorite").asBoolean())
        org.junit.jupiter.api.Assertions.assertEquals(12.5, tree.path("hoursPlayed").asDouble())
    }

    @Test
    fun `anadir duplicado devuelve 409`() {
        val session = session()
        val gameId = createGame("UnicoLib_$suffix")
        addGame(session.token, gameId)
        mockMvc.perform(
            post("/api/library")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to gameId))),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("El juego 'UnicoLib_$suffix' ya está en la biblioteca del usuario ${session.userId}"))
    }

    @Test
    fun `anadir con juego inexistente devuelve 404`() {
        val session = session()
        mockMvc.perform(
            post("/api/library")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to 999999999))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `anadir sin gameId devuelve 400`() {
        val session = session()
        mockMvc.perform(
            post("/api/library")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("isFavorite" to true))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `anadir sin token devuelve 401`() {
        mockMvc.perform(post("/api/library").contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("gameId" to 1))))
            .andExpect(status().isUnauthorized)
    }

    // ===== Obtener =====

    @Test
    fun `obtener entrada devuelve 200`() {
        val session = session()
        val gameId = createGame("Detalle_$suffix")
        addGame(session.token, gameId, isFavorite = true)
        mockMvc.perform(get("/api/library/$gameId").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.gameId").value(gameId))
            .andExpect(jsonPath("$.userId").value(session.userId))
            .andExpect(jsonPath("$.isFavorite").value(true))
    }

    @Test
    fun `entrada inexistente devuelve 404`() {
        val session = session()
        mockMvc.perform(get("/api/library/999999999").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isNotFound)
    }

    // ===== Listar y buscar =====

    @Test
    fun `listar biblioteca vacia devuelve lista vacia`() {
        val session = session()
        createGame("CatLibUno_$suffix")
        createGame("CatLibDos_$suffix")
        mockMvc.perform(get("/api/library").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `listar biblioteca con juegos devuelve los anadidos`() {
        val session = session()
        val gameId = createGame("Tengo_$suffix")
        addGame(session.token, gameId)
        mockMvc.perform(get("/api/library").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].game.name", hasItem("Tengo_$suffix")))
    }

    @Test
    fun `filtrar por favoritos devuelve solo los marcados`() {
        val session = session()
        val favGame = "Fav_$suffix"
        val normalGame = "Normal_$suffix"
        addGame(session.token, createGame(favGame), isFavorite = true)
        addGame(session.token, createGame(normalGame))
        mockMvc.perform(get("/api/library").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}").param("favorites", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].game.name", hasItem(favGame)))
            .andExpect(jsonPath("$[*].game.name", not(hasItem(normalGame))))
    }

    @Test
    fun `buscar por nombre de juego dentro de la biblioteca`() {
        val session = session()
        val match = "Quiero_$suffix"
        val other = "Otra_$suffix"
        addGame(session.token, createGame(match))
        addGame(session.token, createGame(other))
        mockMvc.perform(get("/api/library").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}").param("name", "quiero"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].game.name", hasItem(match)))
            .andExpect(jsonPath("$[*].game.name", not(hasItem(other))))
    }

    @Test
    fun `listar con token invalido devuelve 401`() {
        mockMvc.perform(get("/api/library").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
            .andExpect(status().isUnauthorized)
    }

    // ===== Actualizar =====

    @Test
    fun `marcar favorito y horas se actualizan por separado`() {
        val session = session()
        val gameId = createGame("Editable_$suffix")
        addGame(session.token, gameId)
        mockMvc.perform(
            put("/api/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("isFavorite" to true))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isFavorite").value(true))
            .andExpect(jsonPath("$.hoursPlayed").value(0.0))

        mockMvc.perform(
            put("/api/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("hoursPlayed" to 42.5))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isFavorite").value(true))
            .andExpect(jsonPath("$.hoursPlayed").value(42.5))
    }

    @Test
    fun `actualizar entrada inexistente devuelve 404`() {
        val session = session()
        mockMvc.perform(
            put("/api/library/999999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("isFavorite" to true))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `horas negativas devuelven 400`() {
        val session = session()
        val gameId = createGame("Negativo_$suffix")
        addGame(session.token, gameId)
        mockMvc.perform(
            put("/api/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("hoursPlayed" to -5.0))),
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Borrar =====

    @Test
    fun `borrar entrada devuelve 204 y deja de existir`() {
        val session = session()
        val gameId = createGame("BorraLib_$suffix")
        addGame(session.token, gameId)
        mockMvc.perform(delete("/api/library/$gameId").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/library/$gameId").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `borrar entrada inexistente devuelve 404`() {
        val session = session()
        mockMvc.perform(delete("/api/library/999999999").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isNotFound)
    }
}