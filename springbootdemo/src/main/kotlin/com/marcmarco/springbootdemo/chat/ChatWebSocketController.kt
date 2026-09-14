package com.marcmarco.springbootdemo.chat

import com.marcmarco.springbootdemo.chat.dto.MessageRequest
import com.marcmarco.springbootdemo.chat.dto.MessageResponse
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(
    private val chatService: ChatService,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @MessageMapping("/chat/{conversationId}")
    fun sendMessage(
        @DestinationVariable conversationId: Long,
        message: MessageRequest,
    ) {
        val saved = chatService.saveMessage(conversationId, message)
        messagingTemplate.convertAndSend("/topic/conversations/$conversationId", MessageResponse.from(saved))
    }
}