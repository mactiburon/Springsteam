package com.marcmarco.auth.dto

import com.marcmarco.auth.user.User
import java.time.Instant

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val displayName: String?,
    val bio: String?,
    val avatar: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = requireNotNull(user.id),
            username = user.username,
            email = user.email,
            displayName = user.displayName,
            bio = user.bio,
            avatar = user.avatar,
            createdAt = user.createdAt,
        )
    }
}