package com.marcmarco.springbootdemo.chat

import com.marcmarco.springbootdemo.common.exception.BadRequestException
import com.marcmarco.springbootdemo.common.exception.ConflictException
import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.chat.dto.ConversationRequest
import com.marcmarco.springbootdemo.chat.dto.MessageRequest
import com.marcmarco.springbootdemo.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
) {

    private fun findUser(id: Long) =
        userRepository.findById(id).orElseThrow { NotFoundException("Usuario con id $id no encontrado") }

    private fun isParticipant(conversation: Conversation, userId: Long): Boolean =
        conversation.initiator.id == userId || conversation.participant.id == userId

    /**
     * Devuelve la conversación entre dos usuarios. Si ya existe (en cualquiera de las dos
     * direcciones) la reutiliza; si no, la crea. Idempotente por diseño.
     */
    fun getOrCreateConversation(request: ConversationRequest): Pair<Conversation, Boolean> {
        if (request.initiatorId == request.participantId) {
            throw BadRequestException("No puedes crear una conversación contigo mismo")
        }
        findUser(request.initiatorId)
        findUser(request.participantId)

        conversationRepository.findBetween(request.initiatorId, request.participantId)?.let {
            return Pair(it, false)
        }

        val conversation = conversationRepository.save(
            Conversation(
                initiator = findUser(request.initiatorId),
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

    fun saveMessage(conversationId: Long, request: MessageRequest): Message {
        val conversation = getConversation(conversationId)
        if (!isParticipant(conversation, request.senderId)) {
            throw BadRequestException("El usuario ${request.senderId} no participa en esta conversación")
        }
        val sender = findUser(request.senderId)
        return messageRepository.save(
            Message(conversation = conversation, sender = sender, content = request.content.trim()),
        )
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