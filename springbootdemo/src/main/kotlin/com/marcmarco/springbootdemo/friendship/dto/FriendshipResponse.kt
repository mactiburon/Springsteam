package com.marcmarco.springbootdemo.friendship.dto

import com.marcmarco.springbootdemo.friendship.Friendship
import com.marcmarco.springbootdemo.friendship.FriendshipStatus
import com.marcmarco.springbootdemo.user.dto.UserResponse
import java.time.Instant

data class FriendshipResponse(
    val id: Long,
    val requesterId: Long,
    val addresseeId: Long,
    val requester: UserResponse,
    val addressee: UserResponse,
    val status: FriendshipStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(friendship: Friendship): FriendshipResponse = FriendshipResponse(
            id = requireNotNull(friendship.id),
            requesterId = requireNotNull(friendship.requester.id),
            addresseeId = requireNotNull(friendship.addressee.id),
            requester = UserResponse.from(friendship.requester),
            addressee = UserResponse.from(friendship.addressee),
            status = friendship.status,
            createdAt = friendship.createdAt,
            updatedAt = friendship.updatedAt,
        )
    }
}