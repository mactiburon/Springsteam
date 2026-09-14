package com.marcmarco.springbootdemo.chat

import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
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

    private fun createConversation(user: Session, other: Session, expectedStatus: Int = 201): Long {
        val body = jsonBody(mapOf("participantId" to other.userId))
        val response = mockMvc.perform(
            post("/api/conversations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().`is`(expectedStatus))
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun sendMessage(sender: Session, conversationId: Long, content: String) {
        val body = jsonBody(mapOf("content" to content))
        mockMvc.perform(
            post("/api/conversations/$conversationId/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${sender.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
    }

    // ===== Crear conversación =====

    @Test
    fun `crear conversacion devuelve 201`() {
        val alice = registerLogin("convA")
        val bob = registerLogin("convB")
        mockMvc.perform(
            post("/api/conversations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("participantId" to bob.userId))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.initiator.username").value("convA_$suffix"))
            .andExpect(jsonPath("$.participant.username").value("convB_$suffix"))
    }

    @Test
    fun `conversacion ya existente en direccion inversa devuelve 200 y mismo id`() {
        val alice = registerLogin("inversA")
        val bob = registerLogin("inversB")
        val id = createConversation(alice, bob)
        val id2 = createConversation(bob, alice, expectedStatus = 200)
        org.junit.jupiter.api.Assertions.assertEquals(id, id2)
    }

    @Test
    fun `no puedes crear conversacion contigo mismo devuelve 400`() {
        val alice = registerLogin("soloC")
        mockMvc.perform(
            post("/api/conversations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("participantId" to alice.userId))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `conversacion con usuario inexistente devuelve 404`() {
        val alice = registerLogin("fantC")
        mockMvc.perform(
            post("/api/conversations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("participantId" to 999999999))),
        )
            .andExpect(status().isNotFound)
    }

    // ===== Listar y buscar entre =====

    @Test
    fun `listar conversaciones de un usuario incluye ultimo mensaje`() {
        val alice = registerLogin("listConvA")
        val bob = registerLogin("listConvB")
        val id = createConversation(alice, bob)
        sendMessage(alice, id, "Hola Bob!")
        mockMvc.perform(get("/api/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].id", hasItem(id.toInt())))
            .andExpect(jsonPath("$[0].lastMessage.content").value("Hola Bob!"))
    }

    @Test
    fun `conversacion entre dos usuarios devuelve 200`() {
        val alice = registerLogin("entreA")
        val bob = registerLogin("entreB")
        createConversation(alice, bob)
        mockMvc.perform(
            get("/api/conversations/between")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .param("otherUserId", bob.userId.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.initiator.username").value("entreA_$suffix"))
    }

    @Test
    fun `conversacion entre inexistentes devuelve 404`() {
        val alice = registerLogin("noentreA")
        val bob = registerLogin("noentreB")
        mockMvc.perform(
            get("/api/conversations/between")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .param("otherUserId", bob.userId.toString()),
        )
            .andExpect(status().isNotFound)
    }

    // ===== Mensajes =====

    @Test
    fun `enviar mensaje devuelve 201`() {
        val alice = registerLogin("msgA")
        val bob = registerLogin("msgB")
        val id = createConversation(alice, bob)
        mockMvc.perform(
            post("/api/conversations/$id/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("content" to "Primer hola"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.content").value("Primer hola"))
            .andExpect(jsonPath("$.senderId").value(alice.userId))
            .andExpect(jsonPath("$.conversationId").value(id))
    }

    @Test
    fun `un tercero no puede enviar en la conversacion devuelve 400`() {
        val alice = registerLogin("terConeA")
        val bob = registerLogin("terConeB")
        val carol = registerLogin("terConeC")
        val id = createConversation(alice, bob)
        mockMvc.perform(
            post("/api/conversations/$id/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${carol.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("content" to "intruso"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("El usuario ${carol.userId} no participa en esta conversación"))
    }

    @Test
    fun `mensaje vacio devuelve 400`() {
        val alice = registerLogin("vacioM")
        val bob = registerLogin("vacioM2")
        val id = createConversation(alice, bob)
        mockMvc.perform(
            post("/api/conversations/$id/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("content" to " "))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `listar mensajes devuelve historial en orden`() {
        val alice = registerLogin("histA")
        val bob = registerLogin("histB")
        val id = createConversation(alice, bob)
        sendMessage(alice, id, "msg uno")
        sendMessage(bob, id, "msg dos")
        mockMvc.perform(
            get("/api/conversations/$id/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].content", hasItem("msg uno")))
            .andExpect(jsonPath("$[*].content", hasItem("msg dos")))
    }

    @Test
    fun `un tercero no puede leer mensajes devuelve 400`() {
        val alice = registerLogin("nolectA")
        val bob = registerLogin("nolectB")
        val carol = registerLogin("nolectC")
        val id = createConversation(alice, bob)
        sendMessage(alice, id, "privado")
        mockMvc.perform(
            get("/api/conversations/$id/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${carol.token}"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `conversacion inexistente devuelve 404`() {
        val alice = registerLogin("falle")
        mockMvc.perform(
            get("/api/conversations/999999999/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"),
        )
            .andExpect(status().isNotFound)
    }
}