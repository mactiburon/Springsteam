package com.marcmarco.springbootdemo.chat

import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
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
class ChatControllerIntegrationTest {

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

    private fun createConversation(userId: Long, otherId: Long, expectedStatus: Int = 201): Long {
        val body = jsonBody(mapOf("initiatorId" to userId, "participantId" to otherId))
        val response = mockMvc.perform(
            post("/api/conversations").contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().`is`(expectedStatus))
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun sendMessage(conversationId: Long, senderId: Long, content: String) {
        val body = jsonBody(mapOf("senderId" to senderId, "content" to content))
        mockMvc.perform(
            post("/api/conversations/$conversationId/messages")
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
    }

    // ===== Crear conversación =====

    @Test
    fun `crear conversacion devuelve 201`() {
        val alice = registerUser("convA")
        val bob = registerUser("convB")
        mockMvc.perform(
            post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("initiatorId" to alice, "participantId" to bob))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.initiator.username").value("convA_$suffix"))
            .andExpect(jsonPath("$.participant.username").value("convB_$suffix"))
    }

    @Test
    fun `conversacion ya existente en direccion inversa devuelve 200 y mismo id`() {
        val alice = registerUser("inversA")
        val bob = registerUser("inversB")
        val id = createConversation(alice, bob)
        val id2 = createConversation(bob, alice, expectedStatus = 200)
        org.junit.jupiter.api.Assertions.assertEquals(id, id2)
    }

    @Test
    fun `no puedes crear conversacion contigo mismo devuelve 400`() {
        val alice = registerUser("soloC")
        mockMvc.perform(
            post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("initiatorId" to alice, "participantId" to alice))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `conversacion con usuario inexistente devuelve 404`() {
        val alice = registerUser("fantC")
        mockMvc.perform(
            post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("initiatorId" to alice, "participantId" to 999999999))),
        )
            .andExpect(status().isNotFound)
    }

    // ===== Listar y buscar entre =====

    @Test
    fun `listar conversaciones de un usuario incluye ultimo mensaje`() {
        val alice = registerUser("listConvA")
        val bob = registerUser("listConvB")
        val id = createConversation(alice, bob)
        sendMessage(id, alice, "Hola Bob!")
        mockMvc.perform(get("/api/conversations").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].id", hasItem(id.toInt())))
            .andExpect(jsonPath("$[0].lastMessage.content").value("Hola Bob!"))
    }

    @Test
    fun `conversacion entre dos usuarios devuelve 200`() {
        val alice = registerUser("entreA")
        val bob = registerUser("entreB")
        createConversation(alice, bob)
        mockMvc.perform(get("/api/conversations/between").param("userId", alice.toString()).param("otherUserId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.initiator.username").value("entreA_$suffix"))
    }

    @Test
    fun `conversacion entre inexistentes devuelve 404`() {
        val alice = registerUser("noentreA")
        val bob = registerUser("noentreB")
        mockMvc.perform(get("/api/conversations/between").param("userId", alice.toString()).param("otherUserId", bob.toString()))
            .andExpect(status().isNotFound)
    }

    // ===== Mensajes =====

    @Test
    fun `enviar mensaje devuelve 201`() {
        val alice = registerUser("msgA")
        val bob = registerUser("msgB")
        val id = createConversation(alice, bob)
        mockMvc.perform(
            post("/api/conversations/$id/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("senderId" to alice, "content" to "Primer hola"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.content").value("Primer hola"))
            .andExpect(jsonPath("$.senderId").value(alice))
            .andExpect(jsonPath("$.conversationId").value(id))
    }

    @Test
    fun `un tercero no puede enviar en la conversacion devuelve 400`() {
        val alice = registerUser("terConeA")
        val bob = registerUser("terConeB")
        val carol = registerUser("terConeC")
        val id = createConversation(alice, bob)
        mockMvc.perform(
            post("/api/conversations/$id/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("senderId" to carol, "content" to "intruso"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("El usuario $carol no participa en esta conversación"))
    }

    @Test
    fun `mensaje vacio devuelve 400`() {
        val alice = registerUser("vacioM")
        val bob = registerUser("vacioM2")
        val id = createConversation(alice, bob)
        mockMvc.perform(
            post("/api/conversations/$id/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("senderId" to alice, "content" to " "))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `listar mensajes devuelve historial en orden`() {
        val alice = registerUser("histA")
        val bob = registerUser("histB")
        val id = createConversation(alice, bob)
        sendMessage(id, alice, "msg uno")
        sendMessage(id, bob, "msg dos")
        mockMvc.perform(get("/api/conversations/$id/messages").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].content", hasItem("msg uno")))
            .andExpect(jsonPath("$[*].content", hasItem("msg dos")))
    }

    @Test
    fun `un tercero no puede leer mensajes devuelve 400`() {
        val alice = registerUser("nolectA")
        val bob = registerUser("nolectB")
        val carol = registerUser("nolectC")
        val id = createConversation(alice, bob)
        sendMessage(id, alice, "privado")
        mockMvc.perform(get("/api/conversations/$id/messages").param("userId", carol.toString()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `conversacion inexistente devuelve 404`() {
        val alice = registerUser("falle")
        mockMvc.perform(get("/api/conversations/999999999/messages").param("userId", alice.toString()))
            .andExpect(status().isNotFound)
    }
}