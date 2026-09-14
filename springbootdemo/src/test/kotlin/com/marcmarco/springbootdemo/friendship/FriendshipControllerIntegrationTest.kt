package com.marcmarco.springbootdemo.friendship

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
class FriendshipControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private fun registerUser(prefix: String): Long {
        val body = jsonBody(
            mapOf(
                "username" to "${prefix}_$suffix",
                "email" to "${prefix}_$suffix@test.com",
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

    private fun sendRequest(requesterId: Long, addresseeId: Long): Long {
        val body = jsonBody(mapOf("requesterId" to requesterId, "addresseeId" to addresseeId))
        val response = mockMvc.perform(
            post("/api/friendships").contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    // ===== Enviar solicitud =====

    @Test
    fun `enviar solicitud devuelve 201 con estado PENDING`() {
        val alice = registerUser("alice")
        val bob = registerUser("bob")
        mockMvc.perform(
            post("/api/friendships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("requesterId" to alice, "addresseeId" to bob))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.requester.username").value("alice_$suffix"))
            .andExpect(jsonPath("$.addressee.username").value("bob_$suffix"))
    }

    @Test
    fun `no puedes enviarte solicitud a ti mismo devuelve 400`() {
        val alice = registerUser("solo")
        mockMvc.perform(
            post("/api/friendships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("requesterId" to alice, "addresseeId" to alice))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("No puedes enviarte una solicitud de amistad a ti mismo"))
    }

    @Test
    fun `doble solicitud misma direccion devuelve 409`() {
        val alice = registerUser("dupA")
        val bob = registerUser("dupB")
        sendRequest(alice, bob)
        mockMvc.perform(
            post("/api/friendships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("requesterId" to alice, "addresseeId" to bob))),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `solicitud inversa ya existente devuelve 409`() {
        val alice = registerUser("invA")
        val bob = registerUser("invB")
        sendRequest(alice, bob)
        mockMvc.perform(
            post("/api/friendships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("requesterId" to bob, "addresseeId" to alice))),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Ya existe una relación entre los usuarios $alice y $bob"))
    }

    @Test
    fun `solicitud con usuario inexistente devuelve 404`() {
        val alice = registerUser("fantA")
        mockMvc.perform(
            post("/api/friendships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("requesterId" to alice, "addresseeId" to 999999999))),
        )
            .andExpect(status().isNotFound)
    }

    // ===== Aceptar =====

    @Test
    fun `aceptar solicitud devuelve 200 y cambia a ACCEPTED`() {
        val alice = registerUser("acepA")
        val bob = registerUser("acepB")
        val id = sendRequest(alice, bob)
        mockMvc.perform(put("/api/friendships/$id/accept").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
    }

    @Test
    fun `solo el destinatario puede aceptar devuelve 400`() {
        val alice = registerUser("destA")
        val bob = registerUser("destB")
        val id = sendRequest(alice, bob)
        mockMvc.perform(put("/api/friendships/$id/accept").param("userId", alice.toString()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `aceptar solicitud no pendiente devuelve 409`() {
        val alice = registerUser("nopenA")
        val bob = registerUser("nopenB")
        val id = sendRequest(alice, bob)
        mockMvc.perform(put("/api/friendships/$id/accept").param("userId", bob.toString()))
            .andExpect(status().isOk)
        mockMvc.perform(put("/api/friendships/$id/accept").param("userId", bob.toString()))
            .andExpect(status().isConflict)
    }

    // ===== Bloquear =====

    @Test
    fun `bloquear devuelve 200 y cambia a BLOCKED`() {
        val alice = registerUser("bloqA")
        val bob = registerUser("bloqB")
        val id = sendRequest(alice, bob)
        mockMvc.perform(put("/api/friendships/$id/block").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("BLOCKED"))
    }

    @Test
    fun `un tercero no puede bloquear devuelve 400`() {
        val alice = registerUser("terA")
        val bob = registerUser("terB")
        val carol = registerUser("terC")
        val id = sendRequest(alice, bob)
        mockMvc.perform(put("/api/friendships/$id/block").param("userId", carol.toString()))
            .andExpect(status().isBadRequest)
    }

    // ===== Listar =====

    @Test
    fun `listar relaciones de un usuario devuelve todas`() {
        val alice = registerUser("listA")
        val bob = registerUser("listB")
        sendRequest(alice, bob)
        mockMvc.perform(get("/api/friendships").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].status", hasItem("PENDING")))
    }

    @Test
    fun `filtrar por estado devuelve solo las que coinciden`() {
        val alice = registerUser("filtA")
        val bob = registerUser("filtB")
        val carol = registerUser("filtC")
        val id = sendRequest(alice, bob)
        sendRequest(alice, carol)
        mockMvc.perform(put("/api/friendships/$id/accept").param("userId", bob.toString()))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/friendships").param("userId", alice.toString()).param("status", "ACCEPTED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].status", hasItem("ACCEPTED")))
            .andExpect(jsonPath("$[*].status", not(hasItem("PENDING"))))
    }

    @Test
    fun `listar relaciones de un lado como addressee las incluye`() {
        val alice = registerUser("ladoA")
        val bob = registerUser("ladoB")
        sendRequest(alice, bob)
        mockMvc.perform(get("/api/friendships").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].requester.username").value("ladoA_$suffix"))
    }

    // ===== Borrar =====

    @Test
    fun `borrar relacion devuelve 204 y desaparece`() {
        val alice = registerUser("borrA")
        val bob = registerUser("borrB")
        val id = sendRequest(alice, bob)
        mockMvc.perform(delete("/api/friendships/$id").param("userId", alice.toString()))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/friendships/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `un tercero no puede borrar devuelve 400`() {
        val alice = registerUser("tborrA")
        val bob = registerUser("tborrB")
        val carol = registerUser("tborrC")
        val id = sendRequest(alice, bob)
        mockMvc.perform(delete("/api/friendships/$id").param("userId", carol.toString()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `relacion inexistente devuelve 404`() {
        mockMvc.perform(get("/api/friendships/999999999"))
            .andExpect(status().isNotFound)
    }
}