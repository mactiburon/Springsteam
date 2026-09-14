package com.marcmarco.springbootdemo.nickname

import com.marcmarco.springbootdemo.common.exception.BadRequestException
import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.friendship.FriendshipRepository
import com.marcmarco.springbootdemo.friendship.FriendshipStatus
import com.marcmarco.springbootdemo.nickname.dto.NicknameRequest
import com.marcmarco.springbootdemo.user.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class NicknameService(
    private val nicknameRepository: NicknameRepository,
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
) {

    private fun findUser(id: Long) =
        userRepository.findById(id).orElseThrow { NotFoundException("Usuario con id $id no encontrado") }

    private fun areFriends(userId: Long, otherId: Long): Boolean =
        friendshipRepository.existsByRequesterIdAndAddresseeIdAndStatus(userId, otherId, FriendshipStatus.ACCEPTED) ||
            friendshipRepository.existsByRequesterIdAndAddresseeIdAndStatus(otherId, userId, FriendshipStatus.ACCEPTED)

    fun setNickname(ownerId: Long, targetId: Long, request: NicknameRequest): Nickname {
        if (ownerId == targetId) {
            throw BadRequestException("No puedes ponerte un mote a ti mismo")
        }
        findUser(ownerId)
        findUser(targetId)
        if (!areFriends(ownerId, targetId)) {
            throw BadRequestException("Solo puedes poner un mote a un usuario con el que seas amigo")
        }

        val nickname = nicknameRepository.findByOwnerIdAndTargetId(ownerId, targetId)
            ?.also {
                it.nickname = request.nickname
                it.updatedAt = Instant.now()
            }
            ?: Nickname(
                owner = findUser(ownerId),
                target = findUser(targetId),
                nickname = request.nickname,
            )
        return nicknameRepository.save(nickname)
    }

    fun getNickname(ownerId: Long, targetId: Long): Nickname =
        nicknameRepository.findByOwnerIdAndTargetId(ownerId, targetId)
            ?: throw NotFoundException("No hay mote del usuario $ownerId hacia $targetId")

    fun listNicknames(ownerId: Long): List<Nickname> {
        findUser(ownerId)
        return nicknameRepository.findByOwnerId(ownerId)
    }

    fun removeNickname(ownerId: Long, targetId: Long) {
        val nickname = getNickname(ownerId, targetId)
        nicknameRepository.delete(nickname)
    }
}