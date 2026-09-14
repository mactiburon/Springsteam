package com.marcmarco.springbootdemo.library

import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
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

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private fun registerUser(): Long {
        val body = jsonBody(
            mapOf(
                "username" to "player_$suffix",
                "email" to "$suffix@test.com",
                "password" to "secreto123",
            ),
        )
        val response = mockMvc.perform(
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun createGame(name: String): Long {
        val body = jsonBody(mapOf("name" to name))
        val response = mockMvc.perform(
            post("/api/games").contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun addGame(userId: Long, gameId: Long, isFavorite: Boolean = false, hoursPlayed: Double? = null): String {
        val body = jsonBody(
            mapOf(
                "userId" to userId,
                "gameId" to gameId,
                "isFavorite" to isFavorite,
                "hoursPlayed" to hoursPlayed,
            ),
        )
        return mockMvc.perform(post("/api/library").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString
    }

    // ===== Añadir =====

    @Test
    fun `anadir juego a la biblioteca devuelve 201 con el juego anidado`() {
        val userId = registerUser()
        val gameId = createGame("Almas_$suffix")
        val response = addGame(userId, gameId, isFavorite = true, hoursPlayed = 12.5)
        val tree = objectMapper.readTree(response)
        org.junit.jupiter.api.Assertions.assertEquals(gameId, tree.path("gameId").asLong())
        org.junit.jupiter.api.Assertions.assertEquals("Almas_$suffix", tree.path("game").path("name").asString())
        org.junit.jupiter.api.Assertions.assertEquals(true, tree.path("isFavorite").asBoolean())
        org.junit.jupiter.api.Assertions.assertEquals(12.5, tree.path("hoursPlayed").asDouble())
    }

    @Test
    fun `anadir duplicado devuelve 409`() {
        val userId = registerUser()
        val gameId = createGame("UnicoLib_$suffix")
        addGame(userId, gameId)
        mockMvc.perform(
            post("/api/library")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("userId" to userId, "gameId" to gameId))),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("El juego 'UnicoLib_$suffix' ya está en la biblioteca del usuario $userId"))
    }

    @Test
    fun `anadir con usuario inexistente devuelve 404`() {
        val gameId = createGame("SinUsuario_$suffix")
        mockMvc.perform(
            post("/api/library")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("userId" to 999999999, "gameId" to gameId))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `anadir con juego inexistente devuelve 404`() {
        val userId = registerUser()
        mockMvc.perform(
            post("/api/library")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("userId" to userId, "gameId" to 999999999))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `anadir sin userId devuelve 400`() {
        val gameId = createGame("SinId_$suffix")
        mockMvc.perform(
            post("/api/library")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to gameId))),
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Obtener =====

    @Test
    fun `obtener entrada por userId y gameId devuelve 200`() {
        val userId = registerUser()
        val gameId = createGame("Detalle_$suffix")
        addGame(userId, gameId, isFavorite = true)
        mockMvc.perform(get("/api/library/$gameId").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.gameId").value(gameId))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.isFavorite").value(true))
    }

    @Test
    fun `entrada inexistente devuelve 404`() {
        val userId = registerUser()
        mockMvc.perform(get("/api/library/999999999").param("userId", userId.toString()))
            .andExpect(status().isNotFound)
    }

    // ===== Listar y buscar =====

    @Test
    fun `listar biblioteca devuelve todos los juegos del usuario`() {
        val userId = registerUser()
        createGame("CatLibUno_$suffix")
        createGame("CatLibDos_$suffix")
        mockMvc.perform(get("/api/library").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `listar biblioteca con juegos devuelve los anadidos`() {
        val userId = registerUser()
        val gameId = createGame("Tengo_$suffix")
        addGame(userId, gameId)
        mockMvc.perform(get("/api/library").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].game.name", hasItem("Tengo_$suffix")))
    }

    @Test
    fun `filtrar por favoritos devuelve solo los marcados`() {
        val userId = registerUser()
        val favGame = "Fav_$suffix"
        val normalGame = "Normal_$suffix"
        addGame(userId, createGame(favGame), isFavorite = true)
        addGame(userId, createGame(normalGame))
        mockMvc.perform(get("/api/library").param("userId", userId.toString()).param("favorites", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].game.name", hasItem(favGame)))
            .andExpect(jsonPath("$[*].game.name", not(hasItem(normalGame))))
    }

    @Test
    fun `buscar por nombre de juego dentro de la biblioteca`() {
        val userId = registerUser()
        val match = "Quiero_$suffix"
        val other = "Otra_$suffix"
        addGame(userId, createGame(match))
        addGame(userId, createGame(other))
        mockMvc.perform(get("/api/library").param("userId", userId.toString()).param("name", "quiero"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].game.name", hasItem(match)))
            .andExpect(jsonPath("$[*].game.name", not(hasItem(other))))
    }

    @Test
    fun `listar biblioteca de usuario inexistente devuelve 404`() {
        mockMvc.perform(get("/api/library").param("userId", "999999999"))
            .andExpect(status().isNotFound)
    }

    // ===== Actualizar =====

    @Test
    fun `marcar favorito y horas se actualizan por separado`() {
        val userId = registerUser()
        val gameId = createGame("Editable_$suffix")
        addGame(userId, gameId)
        mockMvc.perform(
            put("/api/library/$gameId")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("isFavorite" to true))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isFavorite").value(true))
            .andExpect(jsonPath("$.hoursPlayed").value(0.0))

        mockMvc.perform(
            put("/api/library/$gameId")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("hoursPlayed" to 42.5))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isFavorite").value(true))
            .andExpect(jsonPath("$.hoursPlayed").value(42.5))
    }

    @Test
    fun `actualizar entrada inexistente devuelve 404`() {
        val userId = registerUser()
        mockMvc.perform(
            put("/api/library/999999999")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("isFavorite" to true))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `horas negativas devuelven 400`() {
        val userId = registerUser()
        val gameId = createGame("Negativo_$suffix")
        addGame(userId, gameId)
        mockMvc.perform(
            put("/api/library/$gameId")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("hoursPlayed" to -5.0))),
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Borrar =====

    @Test
    fun `borrar entrada devuelve 204 y deja de existir`() {
        val userId = registerUser()
        val gameId = createGame("BorraLib_$suffix")
        addGame(userId, gameId)
        mockMvc.perform(delete("/api/library/$gameId").param("userId", userId.toString()))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/library/$gameId").param("userId", userId.toString()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `borrar entrada inexistente devuelve 404`() {
        val userId = registerUser()
        mockMvc.perform(delete("/api/library/999999999").param("userId", userId.toString()))
            .andExpect(status().isNotFound)
    }
}