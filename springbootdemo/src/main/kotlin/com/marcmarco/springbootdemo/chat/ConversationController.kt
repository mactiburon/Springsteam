package com.marcmarco.springbootdemo.chat

import com.marcmarco.springbootdemo.chat.dto.ConversationRequest
import com.marcmarco.springbootdemo.chat.dto.ConversationResponse
import com.marcmarco.springbootdemo.chat.dto.MessageRequest
import com.marcmarco.springbootdemo.chat.dto.MessageResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
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
    fun createConversation(@Valid @RequestBody request: ConversationRequest): ResponseEntity<ConversationResponse> {
        val (conversation, created) = chatService.getOrCreateConversation(request)
        val status = if (created) HttpStatus.CREATED else HttpStatus.OK
        val lastMessage = chatService.getLastMessage(requireNotNull(conversation.id))
        return ResponseEntity.status(status).body(ConversationResponse.from(conversation, lastMessage))
    }

    @GetMapping
    fun listConversations(@RequestParam userId: Long): List<ConversationResponse> =
        chatService.listConversations(userId).map { conv ->
            ConversationResponse.from(conv, chatService.getLastMessage(requireNotNull(conv.id)))
        }

    @GetMapping("/between")
    fun getConversationBetween(
        @RequestParam userId: Long,
        @RequestParam otherUserId: Long,
    ): ConversationResponse {
        val conversation = chatService.getConversationBetween(userId, otherUserId)
        return ConversationResponse.from(conversation, chatService.getLastMessage(requireNotNull(conversation.id)))
    }

    @GetMapping("/{conversationId}/messages")
    fun getMessages(
        @PathVariable conversationId: Long,
        @RequestParam userId: Long,
        @RequestParam(required = false, defaultValue = "100") limit: Int,
    ): List<MessageResponse> =
        chatService.getMessages(conversationId, userId, limit).map { MessageResponse.from(it) }

    @PostMapping("/{conversationId}/messages")
    fun sendMessage(
        @PathVariable conversationId: Long,
        @Valid @RequestBody request: MessageRequest,
    ): ResponseEntity<MessageResponse> {
        val message = chatService.saveMessage(conversationId, request)
        val response = MessageResponse.from(message)
        messagingTemplate.convertAndSend("/topic/conversations/$conversationId", response)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}