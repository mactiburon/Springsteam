package com.marcmarco.audit

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AuditEventRepository : JpaRepository<AuditEvent, Long> {

    @Query(
        """
        SELECT a FROM AuditEvent a
        WHERE (:topic = '' OR a.topic = :topic)
          AND (:type = '' OR a.type = :type)
          AND (:userId IS NULL OR a.userId = :userId)
          AND (:gameId IS NULL OR a.gameId = :gameId)
        ORDER BY a.occurredAt DESC, a.id DESC
        """
    )
    fun search(
        @Param("topic") topic: String,
        @Param("type") type: String,
        @Param("userId") userId: Long?,
        @Param("gameId") gameId: Long?,
        pageable: Pageable,
    ): List<AuditEvent>
}