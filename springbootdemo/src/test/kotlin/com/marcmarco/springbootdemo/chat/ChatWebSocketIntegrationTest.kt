package com.marcmarco.springbootdemo.chat

import com.marcmarco.springbootdemo.chat.dto.MessageRequest
import com.marcmarco.springbootdemo.chat.dto.MessageResponse
import com.marcmarco.springbootdemo.user.User
import com.marcmarco.springbootdemo.user.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.messaging.converter.JacksonJsonMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var conversationRepository: ConversationRepository

    @Autowired
    lateinit var messageRepository: MessageRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private var sessions = mutableListOf<StompSession>()

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

    @AfterEach
    fun tearDown() {
        sessions.forEach { runCatching { it.disconnect() } }
        sessions.clear()
    }

    private fun connect(token: String): StompSession {
        val client = WebSocketStompClient(StandardWebSocketClient())
        client.messageConverter = JacksonJsonMessageConverter(objectMapper as JsonMapper)
        val handler = object : StompSessionHandlerAdapter() {}

        val connectHeaders = StompHeaders().apply { add("Authorization", "Bearer $token") }
        val handshakeHeaders = WebSocketHttpHeaders().apply { add(HttpHeaders.AUTHORIZATION, "Bearer $token") }

        return client.connectAsync("ws://localhost:$port/ws", handshakeHeaders, connectHeaders, handler)
            .get(10, TimeUnit.SECONDS)
            .also { sessions.add(it) }
    }

    private fun userOf(session: Session): User =
        userRepository.findById(session.userId).orElseThrow()

    private fun conversationBetween(a: User, b: User): Conversation =
        conversationRepository.save(Conversation(initiator = a, participant = b))

    private fun subscribeToConversation(
        session: StompSession,
        conversationId: Long,
        queue: LinkedBlockingQueue<MessageResponse>,
        name: String,
    ) {
        session.subscribe(
            "/topic/conversations/$conversationId",
            object : StompFrameHandler {
                override fun getPayloadType(headers: StompHeaders): Class<*> = MessageResponse::class.java
                override fun handleFrame(headers: StompHeaders, payload: Any?) {
                    queue.add(payload as MessageResponse)
                    println("[ws-test-$name] recibido: $payload")
                }
            },
        )
        Thread.sleep(500)
    }

    @Test
    fun `enviar y recibir mensaje por WebSocket en tiempo real`() {
        val alice = registerLogin("ws_alice")
        val bob = registerLogin("ws_bob")
        val conversation = conversationBetween(userOf(alice), userOf(bob))
        val conversationId = conversation.id!!

        val session = connect(alice.token)
        val queue = LinkedBlockingQueue<MessageResponse>()
        subscribeToConversation(session, conversationId, queue, "bob")

        session.send("/app/chat/$conversationId", MessageRequest(content = "Hola por el socket"))

        val received = queue.poll(10, TimeUnit.SECONDS)
        assertNotNull(received, "No se recibió el mensaje por WebSocket")
        assertEquals("Hola por el socket", received.content)
        assertEquals(alice.userId, received.senderId)
    }

    @Test
    fun `el mensaje enviado por WebSocket se persiste y es recuperable REST`() {
        val alice = registerLogin("ws_persist_alice")
        val bob = registerLogin("ws_persist_bob")
        val conversation = conversationBetween(userOf(alice), userOf(bob))
        val conversationId = conversation.id!!

        val session = connect(bob.token)
        val queue = LinkedBlockingQueue<MessageResponse>()
        subscribeToConversation(session, conversationId, queue, "persist")

        session.send("/app/chat/$conversationId", MessageRequest(content = "Guardo esto"))
        val received = queue.poll(10, TimeUnit.SECONDS)
        assertNotNull(received)

        val stored = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId, org.springframework.data.domain.PageRequest.of(0, 10))
        assertEquals("Guardo esto", stored.single().content)
        assertEquals(bob.userId, stored.single().sender.id)
    }
}