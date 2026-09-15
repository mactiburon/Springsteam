package com.marcmarco.shared.event

import java.time.Instant

/** Nombres canónicos de los topics de Kafka del dominio. */
object Topics {
    const val USER_EVENTS = "user-events"
    const val GAME_EVENTS = "game-events"
    const val LIBRARY_EVENTS = "library-events"
}

/**
 * Envoltura común de los eventos publicados en Kafka. `type` identifica la
 * clase de evento (p. ej. "user.registered") y `payload` los datos relevantes,
 * omitiendo siempre información sensible.
 */
data class EventEnvelope(
    val type: String,
    val occurredAt: Instant,
    val payload: Map<String, Any?>,
) {
    companion object {
        fun of(type: String, payload: Map<String, Any?>): EventEnvelope =
            EventEnvelope(type = type, occurredAt = Instant.now(), payload = payload)
    }
}