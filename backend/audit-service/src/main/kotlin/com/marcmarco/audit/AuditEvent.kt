package com.marcmarco.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Registro de auditoría persistido por evento de dominio consumido de Kafka.
 * `userId`/`gameId` se denormalizan desde el payload para permitir búsquedas
 * filtradas; `payloadJson` conserva el contenido íntegro del evento.
 */
@Entity
@Table(name = "audit_events")
class AuditEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "topic", nullable = false)
    val topic: String = "",

    @Column(name = "event_type", nullable = false)
    val type: String = "",

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "game_id")
    val gameId: Long? = null,

    @Column(name = "payload_json", nullable = false)
    val payloadJson: String = "",

    @Column(name = "occurred_at", nullable = false, updatable = false)
    val occurredAt: Instant = Instant.now(),
)