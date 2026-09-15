package com.marcmarco.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.marcmarco.shared.error.NotFoundException
import com.marcmarco.shared.event.EventEnvelope
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuditService(
    private val auditEventRepository: AuditEventRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun record(topic: String, envelope: EventEnvelope) {
        val payload = envelope.payload
        auditEventRepository.save(
            AuditEvent(
                topic = topic,
                type = envelope.type,
                userId = (payload["userId"] as? Number)?.toLong(),
                gameId = (payload["gameId"] as? Number)?.toLong(),
                payloadJson = objectMapper.writeValueAsString(payload),
                occurredAt = envelope.occurredAt,
            ),
        )
    }

    fun search(
        topic: String? = null,
        type: String? = null,
        userId: Long? = null,
        gameId: Long? = null,
        limit: Int = 100,
    ): List<AuditEvent> =
        auditEventRepository.search(
            topic = topic.orEmpty(),
            type = type.orEmpty(),
            userId = userId,
            gameId = gameId,
            pageable = PageRequest.of(0, limit.coerceIn(1, 1000)),
        )

    fun get(id: Long): AuditEvent =
        auditEventRepository.findById(id)
            .orElseThrow { NotFoundException("Evento de auditoría con id $id no encontrado") }
}