package com.marcmarco.springbootdemo.friendship

import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
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
class FriendshipControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private data class Session(val userId: Long, val token: String)

    private fun registerLogin(prefix: String): Session {
        val username = "${prefix}_$suffix"
        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("username" to username, "email" to "${prefix}_$suffix@test.com", "password" to "secreto123"))),
        )
            .andExpect(status().isCreated)
        val response = mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("identifier" to username, "password" to "secreto123"))),
        )
            .andExpect(status().isOk)
            .andReturn().response
        val tree = objectMapper.readTree(response.contentAsString)
        return Session(userId = tree.path("user").path("id").asLong(), token = tree.path("token").asText())
    }

    private fun sendRequest(requesterToken: String, addresseeId: Long): Long {
        val body = jsonBody(mapOf("addresseeId" to addresseeId))
        val response = mockMvc.perform(
            post("/api/friendships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $requesterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    // ===== Enviar solicitud =====

    @Test
    fun `enviar solicitud devuelve 201 con estado PENDING`() {
        val alice = registerLogin("alice")
        val bob = registerLogin("bob")
        mockMvc.perform(
            post("/api/friendships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("addresseeId" to bob.userId))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.requester.username").value("alice_$suffix"))
            .andExpect(jsonPath("$.addressee.username").value("bob_$suffix"))
    }

    @Test
    fun `no puedes enviarte solicitud a ti mismo devuelve 400`() {
        val alice = registerLogin("solo")
        mockMvc.perform(
            post("/api/friendships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("addresseeId" to alice.userId))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("No puedes enviarte una solicitud de amistad a ti mismo"))
    }

    @Test
    fun `doble solicitud misma direccion devuelve 409`() {
        val alice = registerLogin("dupA")
        val bob = registerLogin("dupB")
        sendRequest(alice.token, bob.userId)
        mockMvc.perform(
            post("/api/friendships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("addresseeId" to bob.userId))),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `solicitud inversa ya existente devuelve 409`() {
        val alice = registerLogin("invA")
        val bob = registerLogin("invB")
        sendRequest(alice.token, bob.userId)
        mockMvc.perform(
            post("/api/friendships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("addresseeId" to alice.userId))),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Ya existe una relación entre los usuarios ${alice.userId} y ${bob.userId}"))
    }

    @Test
    fun `solicitud con usuario inexistente devuelve 404`() {
        val alice = registerLogin("fantA")
        mockMvc.perform(
            post("/api/friendships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("addresseeId" to 999999999))),
        )
            .andExpect(status().isNotFound)
    }

    // ===== Aceptar =====

    @Test
    fun `aceptar solicitud devuelve 200 y cambia a ACCEPTED`() {
        val alice = registerLogin("acepA")
        val bob = registerLogin("acepB")
        val id = sendRequest(alice.token, bob.userId)
        mockMvc.perform(put("/api/friendships/$id/accept").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
    }

    @Test
    fun `solo el destinatario puede aceptar devuelve 400`() {
        val alice = registerLogin("destA")
        val bob = registerLogin("destB")
        val id = sendRequest(alice.token, bob.userId)
        mockMvc.perform(put("/api/friendships/$id/accept").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `aceptar solicitud no pendiente devuelve 409`() {
        val alice = registerLogin("nopenA")
        val bob = registerLogin("nopenB")
        val id = sendRequest(alice.token, bob.userId)
        mockMvc.perform(put("/api/friendships/$id/accept").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
        mockMvc.perform(put("/api/friendships/$id/accept").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isConflict)
    }

    // ===== Bloquear =====

    @Test
    fun `bloquear devuelve 200 y cambia a BLOCKED`() {
        val alice = registerLogin("bloqA")
        val bob = registerLogin("bloqB")
        val id = sendRequest(alice.token, bob.userId)
        mockMvc.perform(put("/api/friendships/$id/block").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("BLOCKED"))
    }

    @Test
    fun `un tercero no puede bloquear devuelve 400`() {
        val alice = registerLogin("terA")
        val bob = registerLogin("terB")
        val carol = registerLogin("terC")
        val id = sendRequest(alice.token, bob.userId)
        mockMvc.perform(put("/api/friendships/$id/block").header(HttpHeaders.AUTHORIZATION, "Bearer ${carol.token}"))
            .andExpect(status().isBadRequest)
    }

    // ===== Listar =====

    @Test
    fun `listar relaciones de un usuario devuelve todas`() {
        val alice = registerLogin("listA")
        val bob = registerLogin("listB")
        sendRequest(alice.token, bob.userId)
        mockMvc.perform(get("/api/friendships").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].status", hasItem("PENDING")))
    }

    @Test
    fun `filtrar por estado devuelve solo las que coinciden`() {
        val alice = registerLogin("filtA")
        val bob = registerLogin("filtB")
        val carol = registerLogin("filtC")
        val id = sendRequest(alice.token, bob.userId)
        sendRequest(alice.token, carol.userId)
        mockMvc.perform(put("/api/friendships/$id/accept").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/friendships").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}").param("status", "ACCEPTED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].status", hasItem("ACCEPTED")))
            .andExpect(jsonPath("$[*].status", not(hasItem("PENDING"))))
    }

    @Test
    fun `listar relaciones de un lado como addressee las incluye`() {
        val alice = registerLogin("ladoA")
        val bob = registerLogin("ladoB")
        sendRequest(alice.token, bob.userId)
        mockMvc.perform(get("/api/friendships").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].requester.username").value("ladoA_$suffix"))
    }

    // ===== Borrar =====

    @Test
    fun `borrar relacion devuelve 204 y desaparece`() {
        val alice = registerLogin("borrA")
        val bob = registerLogin("borrB")
        val id = sendRequest(alice.token, bob.userId)
        mockMvc.perform(delete("/api/friendships/$id").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/friendships/$id").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `un tercero no puede borrar devuelve 400`() {
        val alice = registerLogin("tborrA")
        val bob = registerLogin("tborrB")
        val carol = registerLogin("tborrC")
        val id = sendRequest(alice.token, bob.userId)
        mockMvc.perform(delete("/api/friendships/$id").header(HttpHeaders.AUTHORIZATION, "Bearer ${carol.token}"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `relacion inexistente devuelve 404`() {
        val alice = registerLogin("noex")
        mockMvc.perform(get("/api/friendships/999999999").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isNotFound)
    }
}