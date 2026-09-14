package com.marcmarco.springbootdemo.chat

import com.marcmarco.springbootdemo.chat.dto.ConversationRequest
import com.marcmarco.springbootdemo.chat.dto.ConversationResponse
import com.marcmarco.springbootdemo.chat.dto.MessageRequest
import com.marcmarco.springbootdemo.chat.dto.MessageResponse
import com.marcmarco.springbootdemo.security.userId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val chatService: ChatService,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @PostMapping
    fun createConversation(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: ConversationRequest,
    ): ResponseEntity<ConversationResponse> {
        val (conversation, created) = chatService.getOrCreateConversation(jwt.userId(), request)
        val status = if (created) HttpStatus.CREATED else HttpStatus.OK
        val lastMessage = chatService.getLastMessage(requireNotNull(conversation.id))
        return ResponseEntity.status(status).body(ConversationResponse.from(conversation, lastMessage))
    }

    @GetMapping
    fun listConversations(@AuthenticationPrincipal jwt: Jwt): List<ConversationResponse> =
        chatService.listConversations(jwt.userId()).map { conv ->
            ConversationResponse.from(conv, chatService.getLastMessage(requireNotNull(conv.id)))
        }

    @GetMapping("/between")
    fun getConversationBetween(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam otherUserId: Long,
    ): ConversationResponse {
        val conversation = chatService.getConversationBetween(jwt.userId(), otherUserId)
        return ConversationResponse.from(conversation, chatService.getLastMessage(requireNotNull(conversation.id)))
    }

    @GetMapping("/{conversationId}/messages")
    fun getMessages(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable conversationId: Long,
        @RequestParam(required = false, defaultValue = "100") limit: Int,
    ): List<MessageResponse> =
        chatService.getMessages(conversationId, jwt.userId(), limit).map { MessageResponse.from(it) }

    @PostMapping("/{conversationId}/messages")
    fun sendMessage(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable conversationId: Long,
        @Valid @RequestBody request: MessageRequest,
    ): ResponseEntity<MessageResponse> {
        val message = chatService.saveMessage(conversationId, jwt.userId(), request.content)
        val response = MessageResponse.from(message)
        messagingTemplate.convertAndSend("/topic/conversations/$conversationId", response)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}