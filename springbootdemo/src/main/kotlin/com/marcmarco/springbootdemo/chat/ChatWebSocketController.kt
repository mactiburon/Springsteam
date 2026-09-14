package com.marcmarco.springbootdemo.chat

import com.marcmarco.springbootdemo.chat.dto.MessageRequest
import com.marcmarco.springbootdemo.chat.dto.MessageResponse
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatWebSocketController(
    private val chatService: ChatService,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @MessageMapping("/chat/{conversationId}")
    fun sendMessage(
        @DestinationVariable conversationId: Long,
        principal: Principal,
        message: MessageRequest,
    ) {
        val senderId = requireNotNull(principal.name).toLong()
        val saved = chatService.saveMessage(conversationId, senderId, message.content)
        messagingTemplate.convertAndSend("/topic/conversations/$conversationId", MessageResponse.from(saved))
    }
}