package com.marcmarco.audit.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.marcmarco.audit.AuditEvent
import java.time.Instant

data class AuditResponse(
    val id: Long,
    val topic: String,
    val type: String,
    val userId: Long?,
    val gameId: Long?,
    val payload: Map<String, Any?>,
    val occurredAt: Instant,
) {
    companion object {
        fun from(event: AuditEvent, objectMapper: ObjectMapper): AuditResponse {
            val payload = runCatching {
                objectMapper.readValue(event.payloadJson, Map::class.java).entries
                    .associate { it.key.toString() to it.value }
            }.getOrDefault(emptyMap())
            return AuditResponse(
                id = requireNotNull(event.id),
                topic = event.topic,
                type = event.type,
                userId = event.userId,
                gameId = event.gameId,
                payload = payload,
                occurredAt = event.occurredAt,
            )
        }
    }
}