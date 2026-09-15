package com.marcmarco.library

import org.hamcrest.Matchers.hasSize
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
class LibraryCategoryControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jwtEncoder: JwtEncoder

    @Autowired
    lateinit var gameRepository: GameRepository

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private var userIdCounter = 2_000_000L

    private var gameIdCounter = 800_000L

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private data class Session(val userId: Long, val token: String)

    private fun session(): Session {
        val userId = userIdCounter++
        return Session(userId = userId, token = jwtEncoder.testToken(userId))
    }

    private fun createGameAndAddToLibrary(token: String): Long {
        val id = gameIdCounter++
        gameRepository.save(Game(id = id, name = "CategoriaLib_$suffix"))
        mockMvc.perform(
            post("/api/library")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to id))),
        ).andExpect(status().isCreated)
        return id
    }

    private fun createCategory(token: String, name: String): Long {
        val response = mockMvc.perform(
            post("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("name" to name))),
        )
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString
        return objectMapper.readTree(response).path("id").asLong()
    }

    // ===== Crear =====

    @Test
    fun `crear categoria devuelve 201 con nombre y id`() {
        val session = session()
        val response = mockMvc.perform(
            post("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("name" to "Mis Juegos_-${suffix}"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Mis Juegos_-${suffix}"))
            .andExpect(jsonPath("$.gameCount").value(0))
            .andReturn().response.contentAsString
        val id = objectMapper.readTree(response).path("id").asLong()
        org.junit.jupiter.api.Assertions.assertTrue(id > 0)
    }

    @Test
    fun `crear categoria duplicada ignorando mayusculas devuelve 409`() {
        val session = session()
        createCategory(session.token, "Terror_$suffix")
        mockMvc.perform(
            post("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("name" to "terror_$suffix"))),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `crear la misma categoria con otro usuario es permitido`() {
        val sessionA = session()
        val sessionB = session()
        createCategory(sessionA.token, "Individual_$suffix")
        mockMvc.perform(
            post("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${sessionB.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("name" to "Individual_$suffix"))),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `crear categoria sin nombre devuelve 400`() {
        val session = session()
        mockMvc.perform(
            post("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("name" to "  "))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `crear categoria sin token devuelve 401`() {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("name" to "X"))))
            .andExpect(status().isUnauthorized)
    }

    // ===== Listar =====

    @Test
    fun `listar categorias vacias devuelve lista vacia`() {
        val session = session()
        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `listar categorias devuelve solo las del usuario y con conteo`() {
        val session = session()
        val gameId = createGameAndAddToLibrary(session.token)
        val catId = createCategory(session.token, "Rol_$suffix")
        mockMvc.perform(
            post("/api/categories/$catId/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].name").value("Rol_$suffix"))
            .andExpect(jsonPath("$[0].gameCount").value(1))
    }

    @Test
    fun `listar no muestra categorias de otro usuario`() {
        val sessionA = session()
        val sessionB = session()
        createCategory(sessionA.token, "SoloA_$suffix")
        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer ${sessionB.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ===== Renombrar =====

    @Test
    fun `renombrar categoria devuelve el nuevo nombre`() {
        val session = session()
        val catId = createCategory(session.token, "Antes_$suffix")
        mockMvc.perform(
            put("/api/categories/$catId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("name" to "Despues_$suffix"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Despues_$suffix"))
    }

    @Test
    fun `renombrar a nombre ya usado devuelve 409`() {
        val session = session()
        val catId = createCategory(session.token, "Primera_$suffix")
        createCategory(session.token, "Segunda_$suffix")
        mockMvc.perform(
            put("/api/categories/$catId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("name" to "segunda_$suffix"))),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `renombrar categoria inexistente o de otro usuario devuelve 404`() {
        val sessionA = session()
        val sessionB = session()
        val catId = createCategory(sessionA.token, "Ajeno_$suffix")
        mockMvc.perform(
            put("/api/categories/$catId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${sessionB.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("name" to "Robado_$suffix"))),
        )
            .andExpect(status().isNotFound)
    }

    // ===== Borrar =====

    @Test
    fun `borrar categoria devuelve 204 y deja de listarse`() {
        val session = session()
        val catId = createCategory(session.token, "MUERTA_$suffix")
        mockMvc.perform(delete("/api/categories/$catId").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `borrar categoria de otro usuario devuelve 404`() {
        val sessionA = session()
        val sessionB = session()
        val catId = createCategory(sessionA.token, "NoBorrables_$suffix")
        mockMvc.perform(delete("/api/categories/$catId").header(HttpHeaders.AUTHORIZATION, "Bearer ${sessionB.token}"))
            .andExpect(status().isNotFound)
    }

    // ===== Asignar / desasignar =====

    @Test
    fun `asignar categoria a un juego de la biblioteca la devuelve en la entrada`() {
        val session = session()
        val gameId = createGameAndAddToLibrary(session.token)
        val catId = createCategory(session.token, "Favoritas_$suffix")
        mockMvc.perform(
            post("/api/categories/$catId/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categories[0].name").value("Favoritas_$suffix"))
    }

    @Test
    fun `asignar categoria de otro usuario a mi juego devuelve 404`() {
        val sessionA = session()
        val sessionB = session()
        val gameId = createGameAndAddToLibrary(sessionA.token)
        val catId = createCategory(sessionB.token, "Ajena_$suffix")
        mockMvc.perform(
            post("/api/categories/$catId/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${sessionA.token}"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `asignar categoria a un juego que no esta en mi biblioteca devuelve 404`() {
        val session = session()
        val catId = createCategory(session.token, "SinJuego_$suffix")
        mockMvc.perform(
            post("/api/categories/$catId/library/999999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `desasignar categoria de un juego la quita de la entrada`() {
        val session = session()
        val gameId = createGameAndAddToLibrary(session.token)
        val catId = createCategory(session.token, "Temporal_$suffix")
        mockMvc.perform(
            post("/api/categories/$catId/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            delete("/api/categories/$catId/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categories").isEmpty())
    }

    @Test
    fun `desasignar categoria no asignada devuelve 404`() {
        val session = session()
        val gameId = createGameAndAddToLibrary(session.token)
        val catId = createCategory(session.token, "NoAsignada_$suffix")
        mockMvc.perform(
            delete("/api/categories/$catId/library/$gameId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isNotFound)
    }
}