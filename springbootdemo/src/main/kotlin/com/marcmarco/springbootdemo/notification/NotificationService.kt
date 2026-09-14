package com.marcmarco.springbootdemo.notification

import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.user.User
import com.marcmarco.springbootdemo.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
) {

    private fun findUser(id: Long) =
        userRepository.findById(id).orElseThrow { NotFoundException("Usuario con id $id no encontrado") }

    /**
     * Crea y persiste una notificación para [recipient] cuyo causante es [actor].
     * [referenceId] apunta al recurso relacionado (friendshipId, conversationId, ...).
     */
    fun create(
        recipient: User,
        actor: User,
        type: NotificationType,
        referenceId: Long? = null,
    ): Notification {
        val message = when (type) {
            NotificationType.FRIEND_REQUEST -> "${actor.username} te ha enviado una solicitud de amistad"
            NotificationType.FRIEND_ACCEPTED -> "${actor.username} ha aceptado tu solicitud de amistad"
            NotificationType.NEW_MESSAGE -> "${actor.username} te ha enviado un mensaje"
        }
        return notificationRepository.save(
            Notification(
                recipient = recipient,
                actor = actor,
                type = type,
                message = message,
                referenceId = referenceId,
            ),
        )
    }

    fun list(userId: Long): List<Notification> {
        findUser(userId)
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
    }

    fun unreadCount(userId: Long): Long {
        findUser(userId)
        return notificationRepository.countByRecipientIdAndReadIsFalse(userId)
    }

    fun get(id: Long, userId: Long): Notification {
        findUser(userId)
        return notificationRepository.findByIdAndRecipientId(id, userId)
            .orElseThrow { NotFoundException("Notificación con id $id no encontrada") }
    }

    @Transactional
    fun markRead(id: Long, userId: Long): Notification {
        val notification = get(id, userId)
        notification.read = true
        return notificationRepository.save(notification)
    }

    @Transactional
    fun markAllRead(userId: Long) {
        findUser(userId)
        notificationRepository.findByRecipientIdAndReadIsFalse(userId).forEach { it.read = true }
    }

    @Transactional
    fun delete(id: Long, userId: Long) {
        val notification = get(id, userId)
        notificationRepository.delete(notification)
    }
}