package com.marcmarco.springbootdemo.friendship

import com.marcmarco.springbootdemo.common.exception.BadRequestException
import com.marcmarco.springbootdemo.common.exception.ConflictException
import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.friendship.dto.FriendshipRequest
import com.marcmarco.springbootdemo.notification.NotificationService
import com.marcmarco.springbootdemo.notification.NotificationType
import com.marcmarco.springbootdemo.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class FriendshipService(
    private val friendshipRepository: FriendshipRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
) {

    private fun findUser(id: Long) =
        userRepository.findById(id).orElseThrow { NotFoundException("Usuario con id $id no encontrado") }

    fun sendRequest(userId: Long, request: FriendshipRequest): Friendship {
        if (userId == request.addresseeId) {
            throw BadRequestException("No puedes enviarte una solicitud de amistad a ti mismo")
        }
        val requester = findUser(userId)
        val addressee = findUser(request.addresseeId)

        val exists = friendshipRepository.existsByRequesterIdAndAddresseeId(
            userId, request.addresseeId,
        ) || friendshipRepository.existsByRequesterIdAndAddresseeId(
            request.addresseeId, userId,
        )
        if (exists) {
            val (first, second) = listOf(userId, request.addresseeId).sorted()
            throw ConflictException("Ya existe una relación entre los usuarios $first y $second")
        }

        val saved = friendshipRepository.save(
            Friendship(requester = requester, addressee = addressee),
        )
        notificationService.create(
            recipient = addressee,
            actor = requester,
            type = NotificationType.FRIEND_REQUEST,
            referenceId = saved.id,
        )
        return saved
    }

    fun getFriendship(id: Long): Friendship =
        friendshipRepository.findById(id)
            .orElseThrow { NotFoundException("Relación de amistad con id $id no encontrada") }

    private fun canAct(friendship: Friendship, userId: Long): Boolean =
        friendship.requester.id == userId || friendship.addressee.id == userId

    @Transactional
    fun accept(id: Long, userId: Long): Friendship {
        val friendship = getFriendship(id)
        if (friendship.addressee.id != userId) {
            throw BadRequestException("Solo el destinatario de la solicitud puede aceptarla")
        }
        if (friendship.status != FriendshipStatus.PENDING) {
            throw ConflictException("La solicitud ya no está pendiente de respuesta")
        }
        friendship.status = FriendshipStatus.ACCEPTED
        friendship.updatedAt = Instant.now()
        val saved = friendshipRepository.save(friendship)
        notificationService.create(
            recipient = friendship.requester,
            actor = friendship.addressee,
            type = NotificationType.FRIEND_ACCEPTED,
            referenceId = saved.id,
        )
        return saved
    }

    @Transactional
    fun block(id: Long, userId: Long): Friendship {
        val friendship = getFriendship(id)
        if (!canAct(friendship, userId)) {
            throw BadRequestException("No perteneces a esta relación de amistad")
        }
        friendship.status = FriendshipStatus.BLOCKED
        friendship.updatedAt = Instant.now()
        return friendshipRepository.save(friendship)
    }

    fun remove(id: Long, userId: Long) {
        val friendship = getFriendship(id)
        if (!canAct(friendship, userId)) {
            throw BadRequestException("No perteneces a esta relación de amistad")
        }
        friendshipRepository.delete(friendship)
    }

    fun search(userId: Long, status: FriendshipStatus? = null): List<Friendship> {
        findUser(userId)
        return friendshipRepository.search(userId, status)
    }
}