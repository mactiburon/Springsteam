package com.marcmarco.springbootdemo.chat

import com.marcmarco.springbootdemo.common.exception.BadRequestException
import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.chat.dto.ConversationRequest
import com.marcmarco.springbootdemo.notification.NotificationService
import com.marcmarco.springbootdemo.notification.NotificationType
import com.marcmarco.springbootdemo.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
) {

    private fun findUser(id: Long) =
        userRepository.findById(id).orElseThrow { NotFoundException("Usuario con id $id no encontrado") }

    private fun isParticipant(conversation: Conversation, userId: Long): Boolean =
        conversation.initiator.id == userId || conversation.participant.id == userId

    /**
     * Devuelve la conversación entre el usuario autenticado y [request.participantId].
     * Si ya existe (en cualquiera de las dos direcciones) la reutiliza; si no, la crea.
     * Idempotente por diseño.
     */
    fun getOrCreateConversation(userId: Long, request: ConversationRequest): Pair<Conversation, Boolean> {
        if (userId == request.participantId) {
            throw BadRequestException("No puedes crear una conversación contigo mismo")
        }
        findUser(userId)
        findUser(request.participantId)

        conversationRepository.findBetween(userId, request.participantId)?.let {
            return Pair(it, false)
        }

        val conversation = conversationRepository.save(
            Conversation(
                initiator = findUser(userId),
                participant = findUser(request.participantId),
            ),
        )
        return Pair(conversation, true)
    }

    fun getConversationBetween(userId: Long, otherUserId: Long): Conversation {
        findUser(userId)
        return conversationRepository.findBetween(userId, otherUserId)
            ?: throw NotFoundException("No existe conversación entre $userId y $otherUserId")
    }

    fun getConversation(id: Long): Conversation =
        conversationRepository.findById(id)
            .orElseThrow { NotFoundException("Conversación con id $id no encontrada") }

    fun listConversations(userId: Long): List<Conversation> {
        findUser(userId)
        return conversationRepository.findForUser(userId)
    }

    fun saveMessage(conversationId: Long, senderId: Long, content: String): Message {
        val conversation = getConversation(conversationId)
        if (!isParticipant(conversation, senderId)) {
            throw BadRequestException("El usuario $senderId no participa en esta conversación")
        }
        val sender = findUser(senderId)
        val message = messageRepository.save(
            Message(conversation = conversation, sender = sender, content = content.trim()),
        )
        val recipient = if (conversation.initiator.id == sender.id) conversation.participant else conversation.initiator
        notificationService.create(
            recipient = recipient,
            actor = sender,
            type = NotificationType.NEW_MESSAGE,
            referenceId = conversationId,
        )
        return message
    }

    fun getMessages(conversationId: Long, userId: Long, limit: Int): List<Message> {
        val conversation = getConversation(conversationId)
        if (!isParticipant(conversation, userId)) {
            throw BadRequestException("El usuario $userId no participa en esta conversación")
        }
        return messageRepository.findByConversationIdOrderBySentAtAsc(
            conversationId = conversationId,
            pageable = PageRequest.of(0, limit),
        )
    }

    fun getLastMessage(conversationId: Long): Message? =
        messageRepository.findTopByConversationIdOrderBySentAtDesc(conversationId)
}