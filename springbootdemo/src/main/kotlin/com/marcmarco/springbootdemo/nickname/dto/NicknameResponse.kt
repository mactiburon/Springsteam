package com.marcmarco.springbootdemo.nickname.dto

import com.marcmarco.springbootdemo.nickname.Nickname
import com.marcmarco.springbootdemo.user.dto.UserResponse
import java.time.Instant

data class NicknameResponse(
    val id: Long,
    val ownerId: Long,
    val targetId: Long,
    val target: UserResponse,
    val nickname: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(nickname: Nickname): NicknameResponse = NicknameResponse(
            id = requireNotNull(nickname.id),
            ownerId = requireNotNull(nickname.owner.id),
            targetId = requireNotNull(nickname.target.id),
            target = UserResponse.from(nickname.target),
            nickname = nickname.nickname,
            createdAt = nickname.createdAt,
            updatedAt = nickname.updatedAt,
        )
    }
}