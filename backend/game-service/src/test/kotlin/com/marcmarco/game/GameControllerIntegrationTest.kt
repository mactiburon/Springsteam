package com.marcmarco.game

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
class GameControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jwtEncoder: JwtEncoder

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private var authToken: String? = null

    private fun token(): String {
        authToken?.let { return it }
        return jwtEncoder.testToken().also { authToken = it }
    }

    private fun createGame(
        name: String = "Juego_$suffix",
        genre: String? = null,
        releaseDate: String? = null,
        developer: String? = null,
        publisher: String? = null,
    ): Long {
        val body = jsonBody(
            mapOf(
                "name" to name,
                "genre" to genre,
                "releaseDate" to releaseDate,
                "developer" to developer,
                "publisher" to publisher,
            ),
        )
        val response = mockMvc.perform(
            post("/api/games")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    // ===== Crear =====

    @Test
    fun `crear juego devuelve 201 y persiste los campos`() {
        val name = "Terror_$suffix"
        val body = jsonBody(
            mapOf(
                "name" to name,
                "description" to "Un survival horror tenso",
                "genre" to "Terror",
                "releaseDate" to "2023-10-27",
                "developer" to "Equipo X",
                "publisher" to "Publisher Y",
                "cover" to "https://cdn.com/cover.jpg",
            ),
        )
        mockMvc.perform(post("/api/games").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.genre").value("Terror"))
            .andExpect(jsonPath("$.releaseDate").value("2023-10-27"))
            .andExpect(jsonPath("$.developer").value("Equipo X"))
            .andExpect(jsonPath("$.publisher").value("Publisher Y"))
            .andExpect(jsonPath("$.createdAt").isString)
    }

    @Test
    fun `nombre duplicado devuelve 409`() {
        val name = "Unico_$suffix"
        createGame(name)
        val body = jsonBody(mapOf("name" to name))
        mockMvc.perform(post("/api/games").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Ya existe un juego llamado '$name'"))
    }

    @Test
    fun `nombre vacio devuelve 400`() {
        val body = jsonBody(mapOf("name" to " "))
        mockMvc.perform(post("/api/games").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `crear juego sin campos opcionales usa null`() {
        val name = "Minimo_$suffix"
        createGame(name)
        mockMvc.perform(get("/api/games").param("name", name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").isNumber)
            .andExpect(jsonPath("$[0].genre").value(org.hamcrest.Matchers.nullValue()))
    }

    // ===== Obtener =====

    @Test
    fun `obtener juego por id devuelve 200`() {
        val name = "Ficha_$suffix"
        val id = createGame(name)
        mockMvc.perform(get("/api/games/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value(name))
    }

    @Test
    fun `juego inexistente devuelve 404`() {
        mockMvc.perform(get("/api/games/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Juego con id 999999999 no encontrado"))
    }

    // ===== Actualizar =====

    @Test
    fun `actualizar juego devuelve 200 con los nuevos campos`() {
        val name = "Original_$suffix"
        val id = createGame(name, genre = "Aventura")
        val body = jsonBody(
            mapOf(
                "name" to "Remaster_$suffix",
                "genre" to "RPG",
                "releaseDate" to "2026-01-15",
            ),
        )
        mockMvc.perform(put("/api/games/$id").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Remaster_$suffix"))
            .andExpect(jsonPath("$.genre").value("RPG"))
            .andExpect(jsonPath("$.releaseDate").value("2026-01-15"))
    }

    @Test
    fun `actualizar a un nombre ya existente devuelve 409`() {
        val other = "Ocupado_$suffix"
        createGame(other)
        val id = createGame("Editable_$suffix")
        val body = jsonBody(mapOf("name" to other))
        mockMvc.perform(put("/api/games/$id").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
    }

    @Test
    fun `actualizar juego inexistente devuelve 404`() {
        val body = jsonBody(mapOf("name" to "Fantasma_$suffix"))
        mockMvc.perform(put("/api/games/999999999").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound)
    }

    // ===== Borrar =====

    @Test
    fun `borrar juego devuelve 204 y deja de existir`() {
        val id = createGame()
        mockMvc.perform(delete("/api/games/$id").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/games/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `borrar juego inexistente devuelve 404`() {
        mockMvc.perform(delete("/api/games/999999999").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isNotFound)
    }

    // ===== Buscar =====

    @Test
    fun `listar juegos devuelve todos los creados`() {
        val name = "Catalogo_$suffix"
        createGame(name)
        mockMvc.perform(get("/api/games"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].name", hasItem(name)))
    }

    @Test
    fun `buscar por nombre devuelve solo los que coinciden`() {
        val match = "Half_$suffix"
        val other = "Portal_$suffix"
        createGame(match)
        createGame(other)
        mockMvc.perform(get("/api/games").param("name", "half_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].name", hasItem(match)))
            .andExpect(jsonPath("$[*].name", not(hasItem(other))))
    }

    @Test
    fun `buscar por genero devuelve solo los de ese genero`() {
        val rpg = "RPG_$suffix"
        val shooter = "Shooter_$suffix"
        createGame("Fantasia_$suffix", genre = rpg)
        createGame("Guerra_$suffix", genre = shooter)
        mockMvc.perform(get("/api/games").param("genre", rpg))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].name", hasItem("Fantasia_$suffix")))
            .andExpect(jsonPath("$[*].name", not(hasItem("Guerra_$suffix"))))
    }

    @Test
    fun `buscar combinando nombre y genero aplica ambos filtros`() {
        val rpg = "RPG_C_$suffix"
        createGame("Combina_Uno_$suffix", genre = rpg)
        createGame("Combina_Dos_$suffix", genre = "Shooter")
        createGame("Otro_$suffix", genre = rpg)
        mockMvc.perform(get("/api/games").param("name", "Combina").param("genre", rpg))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].name", hasItem("Combina_Uno_$suffix")))
            .andExpect(jsonPath("$[*].name", not(hasItem("Otro_$suffix"))))
            .andExpect(jsonPath("$[*].name", not(hasItem("Combina_Dos_$suffix"))))
    }

    @Test
    fun `buscar por rango de fechas devuelve los juegos de ese periodo`() {
        val old = "Viejo_$suffix"
        val recent = "Nuevo_$suffix"
        createGame(old, releaseDate = "2008-11-18")
        createGame(recent, releaseDate = "2026-06-01")
        mockMvc.perform(get("/api/games").param("releaseDateFrom", "2020-01-01").param("releaseDateTo", "2026-12-31"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].name", hasItem(recent)))
            .andExpect(jsonPath("$[*].name", not(hasItem(old))))
    }

    @Test
    fun `buscar sin coincidencias devuelve lista vacia`() {
        mockMvc.perform(get("/api/games").param("name", "no_existe_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}