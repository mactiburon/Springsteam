package com.marcmarco.springbootdemo.notification

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
class NotificationControllerIntegrationTest {

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

    private fun sendRequest(requester: Session, addressee: Session): Long {
        val body = jsonBody(mapOf("addresseeId" to addressee.userId))
        val response = mockMvc.perform(
            post("/api/friendships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun acceptRequest(friendshipId: Long, addressee: Session) {
        mockMvc.perform(
            put("/api/friendships/$friendshipId/accept")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${addressee.token}"),
        )
            .andExpect(status().isOk)
    }

    private fun createConversation(initiator: Session, participant: Session): Long {
        val body = jsonBody(mapOf("participantId" to participant.userId))
        val response = mockMvc.perform(
            post("/api/conversations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${initiator.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun sendChatMessage(conversationId: Long, sender: Session, content: String) {
        val body = jsonBody(mapOf("content" to content))
        mockMvc.perform(
            post("/api/conversations/$conversationId/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${sender.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
    }

    // ===== Generación automática =====

    @Test
    fun `solicitud de amistad genera notificacion para el destinatario`() {
        val alice = registerLogin("notiA")
        val bob = registerLogin("notiB")
        val friendshipId = sendRequest(alice, bob)

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("FRIEND_REQUEST"))
            .andExpect(jsonPath("$[0].referenceId").value(friendshipId.toInt()))
            .andExpect(jsonPath("$[0].actor.id").value(alice.userId.toInt()))
            .andExpect(jsonPath("$[0].read").value(false))
            .andExpect(jsonPath("$[0].message").value("notiA_$suffix te ha enviado una solicitud de amistad"))
    }

    @Test
    fun `aceptar solicitud genera notificacion para el solicitante`() {
        val alice = registerLogin("notiC")
        val bob = registerLogin("notiD")
        val friendshipId = sendRequest(alice, bob)
        acceptRequest(friendshipId, bob)

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("FRIEND_ACCEPTED"))
            .andExpect(jsonPath("$[0].referenceId").value(friendshipId.toInt()))
            .andExpect(jsonPath("$[0].actor.id").value(bob.userId.toInt()))
            .andExpect(jsonPath("$[0].message").value("notiD_$suffix ha aceptado tu solicitud de amistad"))
    }

    @Test
    fun `enviar mensaje por REST genera notificacion para el otro participante`() {
        val alice = registerLogin("notiE")
        val bob = registerLogin("notiF")
        val conversationId = createConversation(alice, bob)
        sendChatMessage(conversationId, alice, "Hola Bob!")

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("NEW_MESSAGE"))
            .andExpect(jsonPath("$[0].referenceId").value(conversationId.toInt()))
            .andExpect(jsonPath("$[0].actor.id").value(alice.userId.toInt()))
            .andExpect(jsonPath("$[0].message").value("notiE_$suffix te ha enviado un mensaje"))

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ===== Gestión de lectura =====

    @Test
    fun `unread-count refleja pendientes y se resetea al marcar como leida`() {
        val alice = registerLogin("notiG")
        val bob = registerLogin("notiH")
        sendRequest(alice, bob)

        mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].read").value(false))

        val notificationId = objectMapper.readTree(
            mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
                .andReturn().response.contentAsString,
        ).path(0).path("id").asLong()

        mockMvc.perform(
            put("/api/notifications/$notificationId/read")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.read").value(true))

        mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
    }

    @Test
    fun `read-all marca todas las notificaciones como leidas`() {
        val carol = registerLogin("notiI")
        val dave = registerLogin("notiJ")
        val bob = registerLogin("notiK")
        sendRequest(carol, bob)
        sendRequest(dave, bob)

        mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(2))

        mockMvc.perform(put("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].read").value(true))
            .andExpect(jsonPath("$[1].read").value(true))

        mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
    }

    // ===== Eliminación y acceso =====

    @Test
    fun `eliminar notificacion devuelve 204 y luego 404`() {
        val alice = registerLogin("notiL")
        val bob = registerLogin("notiM")
        sendRequest(alice, bob)

        val notificationId = objectMapper.readTree(
            mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
                .andReturn().response.contentAsString,
        ).path(0).path("id").asLong()

        mockMvc.perform(
            delete("/api/notifications/$notificationId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/notifications/$notificationId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"),
        )
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `un tercero no puede ver la notificacion ajena y devuelve 404`() {
        val alice = registerLogin("notiN")
        val bob = registerLogin("notiO")
        val charlie = registerLogin("notiP")
        sendRequest(alice, bob)

        val notificationId = objectMapper.readTree(
            mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
                .andReturn().response.contentAsString,
        ).path(0).path("id").asLong()

        mockMvc.perform(
            get("/api/notifications/$notificationId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${charlie.token}"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `un tercero no puede marcar ni borrar la notificacion ajena`() {
        val alice = registerLogin("notiQ")
        val bob = registerLogin("notiR")
        val charlie = registerLogin("notiS")
        sendRequest(alice, bob)

        val notificationId = objectMapper.readTree(
            mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
                .andReturn().response.contentAsString,
        ).path(0).path("id").asLong()

        mockMvc.perform(
            put("/api/notifications/$notificationId/read")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${charlie.token}"),
        )
            .andExpect(status().isNotFound)

        mockMvc.perform(
            delete("/api/notifications/$notificationId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${charlie.token}"),
        )
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, "Bearer ${bob.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `sin token devuelve 401`() {
        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/notifications/unread-count"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/notifications/1"))
            .andExpect(status().isUnauthorized)
    }
}