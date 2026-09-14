package com.marcmarco.springbootdemo.friendship

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FriendshipRepository : JpaRepository<Friendship, Long> {

    fun existsByRequesterIdAndAddresseeId(requesterId: Long, addresseeId: Long): Boolean

    @Query(
        """
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
          AND (:status IS NULL OR f.status = :status)
        ORDER BY f.createdAt DESC
        """
    )
    fun search(
        @Param("userId") userId: Long,
        @Param("status") status: FriendshipStatus?,
    ): List<Friendship>
}