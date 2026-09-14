package com.marcmarco.springbootdemo.search

import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SearchControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private fun registerUser(username: String): Long {
        val body = jsonBody(
            mapOf(
                "username" to username,
                "email" to "$username@test.com",
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

    private fun createGame(name: String, genre: String? = null) {
        val body = jsonBody(mapOf("name" to name, "genre" to genre))
        mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
    }

    // ===== Búsqueda global =====

    @Test
    fun `busqueda global devuelve juegos y usuarios que coinciden`() {
        val queryTerm = "nebulosa_$suffix"
        createGame(queryTerm)
        registerUser(queryTerm)
        mockMvc.perform(get("/api/search").param("q", queryTerm))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.games[*].name", hasItem(queryTerm)))
            .andExpect(jsonPath("$.users[*].username", hasItem(queryTerm)))
    }

    @Test
    fun `busqueda global por genero encuentra juegos`() {
        val genre = "PixelArt"
        createGame("Juego_$suffix", genre = genre)
        mockMvc.perform(get("/api/search").param("q", "pixel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.games[*].genre", hasItem(genre)))
    }

    @Test
    fun `busqueda global por username es parcial e insensible a mayusculas`() {
        val username = "Neo_$suffix"
        registerUser(username)
        mockMvc.perform(get("/api/search").param("q", "neo_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.users[*].username", hasItem(username)))
    }

    @Test
    fun `busqueda por nombre de juego devuelve el juego y no usuarios`() {
        val gameName = "Elden_$suffix"
        createGame(gameName)
        registerUser("jugador_a")
        mockMvc.perform(get("/api/search").param("q", gameName))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.games[*].name", hasItem(gameName)))
    }

    @Test
    fun `busqueda sin coincidencias devuelve listas vacias`() {
        mockMvc.perform(get("/api/search").param("q", "term_inexistente_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.games.length()").value(0))
            .andExpect(jsonPath("$.users.length()").value(0))
    }

    @Test
    fun `busqueda con query vacia devuelve listas vacias`() {
        mockMvc.perform(get("/api/search").param("q", "   "))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.games.length()").value(0))
            .andExpect(jsonPath("$.users.length()").value(0))
    }

    // ===== Sugerencias =====

    @Test
    fun `sugerencias mezclan nombres de juegos y usuarios`() {
        val gameName = "Ciclón_$suffix"
        val username = "Ciclope_$suffix"
        createGame(gameName)
        registerUser(username)
        mockMvc.perform(get("/api/search/suggestions").param("q", "Cicl"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*]", hasItem(gameName)))
            .andExpect(jsonPath("$[*]", hasItem(username)))
    }

    @Test
    fun `sugerencias respetan el limite`() {
        repeat(3) { createGame("UnicoUno_${it}_$suffix") }
        mockMvc.perform(get("/api/search/suggestions").param("q", "UnicoUno").param("limit", "2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `sugerencias sin coincidencias devuelven lista vacia`() {
        mockMvc.perform(get("/api/search/suggestions").param("q", "zzz_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}