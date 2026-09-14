package com.marcmarco.springbootdemo.chat

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ConversationRepository : JpaRepository<Conversation, Long> {

    fun existsByInitiatorIdAndParticipantId(initiatorId: Long, participantId: Long): Boolean

    @Query(
        """
        SELECT c FROM Conversation c
        WHERE (c.initiator.id = :u1 AND c.participant.id = :u2)
           OR (c.initiator.id = :u2 AND c.participant.id = :u1)
        """
    )
    fun findBetween(@Param("u1") u1: Long, @Param("u2") u2: Long): Conversation?

    @Query(
        """
        SELECT c FROM Conversation c
        WHERE c.initiator.id = :userId OR c.participant.id = :userId
        ORDER BY c.createdAt DESC
        """
    )
    fun findForUser(@Param("userId") userId: Long): List<Conversation>
}