package com.marcmarco.audit.event

import com.marcmarco.audit.AuditService
import com.marcmarco.shared.event.EventEnvelope
import com.marcmarco.shared.event.Topics
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

/**
 * Absorbe todos los eventos de dominio (usuarios, juegos y biblioteca) y los
 * persiste como registro de auditoría en auditdb.
 */
@Component
@ConditionalOnProperty(name = ["app.events.enabled"], havingValue = "true", matchIfMissing = true)
class AuditEventConsumer(
    private val auditService: AuditService,
) {

    @KafkaListener(
        topics = [Topics.USER_EVENTS, Topics.GAME_EVENTS, Topics.LIBRARY_EVENTS],
        groupId = "audit-service",
    )
    fun onDomainEvent(
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        envelope: EventEnvelope,
    ) {
        auditService.record(topic, envelope)
    }
}