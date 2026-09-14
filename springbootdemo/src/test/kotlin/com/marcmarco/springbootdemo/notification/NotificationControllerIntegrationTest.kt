package com.marcmarco.springbootdemo.notification

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
class NotificationControllerIntegrationTest {

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

    private fun acceptRequest(friendshipId: Long, addresseeId: Long) {
        mockMvc.perform(put("/api/friendships/$friendshipId/accept").param("userId", addresseeId.toString()))
            .andExpect(status().isOk)
    }

    private fun createConversation(initiatorId: Long, participantId: Long): Long {
        val body = jsonBody(mapOf("initiatorId" to initiatorId, "participantId" to participantId))
        val response = mockMvc.perform(
            post("/api/conversations").contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun sendChatMessage(conversationId: Long, senderId: Long, content: String) {
        val body = jsonBody(mapOf("senderId" to senderId, "content" to content))
        mockMvc.perform(
            post("/api/conversations/$conversationId/messages")
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
    }

    // ===== Generación automática =====

    @Test
    fun `solicitud de amistad genera notificacion para el destinatario`() {
        val alice = registerUser("notiA")
        val bob = registerUser("notiB")
        val friendshipId = sendRequest(alice, bob)

        mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("FRIEND_REQUEST"))
            .andExpect(jsonPath("$[0].referenceId").value(friendshipId.toInt()))
            .andExpect(jsonPath("$[0].actor.id").value(alice.toInt()))
            .andExpect(jsonPath("$[0].read").value(false))
            .andExpect(jsonPath("$[0].message").value("notiA_$suffix te ha enviado una solicitud de amistad"))
    }

    @Test
    fun `aceptar solicitud genera notificacion para el solicitante`() {
        val alice = registerUser("notiC")
        val bob = registerUser("notiD")
        val friendshipId = sendRequest(alice, bob)
        acceptRequest(friendshipId, bob)

        mockMvc.perform(get("/api/notifications").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("FRIEND_ACCEPTED"))
            .andExpect(jsonPath("$[0].referenceId").value(friendshipId.toInt()))
            .andExpect(jsonPath("$[0].actor.id").value(bob.toInt()))
            .andExpect(jsonPath("$[0].message").value("notiD_$suffix ha aceptado tu solicitud de amistad"))
    }

    @Test
    fun `enviar mensaje por REST genera notificacion para el otro participante`() {
        val alice = registerUser("notiE")
        val bob = registerUser("notiF")
        val conversationId = createConversation(alice, bob)
        sendChatMessage(conversationId, alice, "Hola Bob!")

        mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("NEW_MESSAGE"))
            .andExpect(jsonPath("$[0].referenceId").value(conversationId.toInt()))
            .andExpect(jsonPath("$[0].actor.id").value(alice.toInt()))
            .andExpect(jsonPath("$[0].message").value("notiE_$suffix te ha enviado un mensaje"))

        mockMvc.perform(get("/api/notifications").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ===== Gestión de lectura =====

    @Test
    fun `unread-count refleja pendientes y se resetea al marcar como leida`() {
        val alice = registerUser("notiG")
        val bob = registerUser("notiH")
        sendRequest(alice, bob)

        mockMvc.perform(get("/api/notifications/unread-count").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))

        mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].read").value(false))

        val notificationId = objectMapper.readTree(
            mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
                .andReturn().response.contentAsString,
        ).path(0).path("id").asLong()

        mockMvc.perform(put("/api/notifications/$notificationId/read").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.read").value(true))

        mockMvc.perform(get("/api/notifications/unread-count").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
    }

    @Test
    fun `read-all marca todas las notificaciones como leidas`() {
        val carol = registerUser("notiI")
        val dave = registerUser("notiJ")
        val bob = registerUser("notiK")
        sendRequest(carol, bob)
        sendRequest(dave, bob)

        mockMvc.perform(get("/api/notifications/unread-count").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(2))

        mockMvc.perform(put("/api/notifications/read-all").param("userId", bob.toString()))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].read").value(true))
            .andExpect(jsonPath("$[1].read").value(true))

        mockMvc.perform(get("/api/notifications/unread-count").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
    }

    // ===== Eliminación y acceso =====

    @Test
    fun `eliminar notificacion devuelve 204 y luego 404`() {
        val alice = registerUser("notiL")
        val bob = registerUser("notiM")
        sendRequest(alice, bob)

        val notificationId = objectMapper.readTree(
            mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
                .andReturn().response.contentAsString,
        ).path(0).path("id").asLong()

        mockMvc.perform(delete("/api/notifications/$notificationId").param("userId", bob.toString()))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/notifications/$notificationId").param("userId", bob.toString()))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `un tercero no puede ver la notificacion ajena y devuelve 404`() {
        val alice = registerUser("notiN")
        val bob = registerUser("notiO")
        val charlie = registerUser("notiP")
        sendRequest(alice, bob)

        val notificationId = objectMapper.readTree(
            mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
                .andReturn().response.contentAsString,
        ).path(0).path("id").asLong()

        mockMvc.perform(get("/api/notifications/$notificationId").param("userId", charlie.toString()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `un tercero no puede marcar ni borrar la notificacion ajena`() {
        val alice = registerUser("notiQ")
        val bob = registerUser("notiR")
        val charlie = registerUser("notiS")
        sendRequest(alice, bob)

        val notificationId = objectMapper.readTree(
            mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
                .andReturn().response.contentAsString,
        ).path(0).path("id").asLong()

        mockMvc.perform(put("/api/notifications/$notificationId/read").param("userId", charlie.toString()))
            .andExpect(status().isNotFound)

        mockMvc.perform(delete("/api/notifications/$notificationId").param("userId", charlie.toString()))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/notifications").param("userId", bob.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `listar de un usuario inexistente devuelve 404`() {
        mockMvc.perform(get("/api/notifications").param("userId", "999999999"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/notifications/unread-count").param("userId", "999999999"))
            .andExpect(status().isNotFound)
    }
}