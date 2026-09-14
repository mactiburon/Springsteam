package com.marcmarco.springbootdemo.chat.dto

import com.marcmarco.springbootdemo.chat.Conversation
import com.marcmarco.springbootdemo.chat.Message
import com.marcmarco.springbootdemo.user.dto.UserResponse
import java.time.Instant

data class MessageResponse(
    val id: Long,
    val conversationId: Long,
    val senderId: Long,
    val senderUsername: String?,
    val content: String,
    val sentAt: Instant,
) {
    companion object {
        fun from(message: Message): MessageResponse = MessageResponse(
            id = requireNotNull(message.id),
            conversationId = requireNotNull(message.conversation.id),
            senderId = requireNotNull(message.sender.id),
            senderUsername = message.sender.username,
            content = message.content,
            sentAt = message.sentAt,
        )
    }
}

data class ConversationResponse(
    val id: Long,
    val initiatorId: Long,
    val participantId: Long,
    val initiator: UserResponse,
    val participant: UserResponse,
    val lastMessage: MessageResponse?,
    val createdAt: Instant,
) {
    companion object {
        fun from(conversation: Conversation, lastMessage: Message?): ConversationResponse = ConversationResponse(
            id = requireNotNull(conversation.id),
            initiatorId = requireNotNull(conversation.initiator.id),
            participantId = requireNotNull(conversation.participant.id),
            initiator = UserResponse.from(conversation.initiator),
            participant = UserResponse.from(conversation.participant),
            lastMessage = lastMessage?.let { MessageResponse.from(it) },
            createdAt = conversation.createdAt,
        )
    }
}