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
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.JacksonJsonMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

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

    @AfterEach
    fun tearDown() {
        sessions.forEach { runCatching { it.disconnect() } }
        sessions.clear()
    }

    private fun connect(): StompSession {
        val client = WebSocketStompClient(StandardWebSocketClient())
        client.messageConverter = JacksonJsonMessageConverter(objectMapper as JsonMapper)
        val handler = object : StompSessionHandlerAdapter() {}
        return client.connectAsync("ws://localhost:$port/ws", handler)
            .get(10, TimeUnit.SECONDS)
            .also { sessions.add(it) }
    }

    private fun newUser(username: String): User =
        userRepository.save(
            User(
                username = username,
                email = "$username@test.com",
                password = "secreto123",
                displayName = username,
            ),
        )

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
        val alice = newUser("ws_alice_$suffix")
        val bob = newUser("ws_bob_$suffix")
        val conversation = conversationBetween(alice, bob)
        val conversationId = conversation.id!!

        val session = connect()
        val queue = LinkedBlockingQueue<MessageResponse>()
        subscribeToConversation(session, conversationId, queue, "bob")

        session.send("/app/chat/$conversationId", MessageRequest(senderId = alice.id!!, content = "Hola por el socket"))

        val received = queue.poll(10, TimeUnit.SECONDS)
        assertNotNull(received, "No se recibió el mensaje por WebSocket")
        assertEquals("Hola por el socket", received.content)
        assertEquals(alice.id, received.senderId)
    }

    @Test
    fun `el mensaje enviado por WebSocket se persiste y es recuperable REST`() {
        val alice = newUser("ws_persist_alice_$suffix")
        val bob = newUser("ws_persist_bob_$suffix")
        val conversation = conversationBetween(alice, bob)
        val conversationId = conversation.id!!

        val session = connect()
        val queue = LinkedBlockingQueue<MessageResponse>()
        subscribeToConversation(session, conversationId, queue, "persist")

        session.send("/app/chat/$conversationId", MessageRequest(senderId = bob.id!!, content = "Guardo esto"))
        val received = queue.poll(10, TimeUnit.SECONDS)
        assertNotNull(received)

        val stored = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId, org.springframework.data.domain.PageRequest.of(0, 10))
        assertEquals("Guardo esto", stored.single().content)
        assertEquals(bob.id, stored.single().sender.id)
    }
}