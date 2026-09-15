package com.marcmarco.shared.event

import java.time.Instant

/** Nombres canónicos de los topics de Kafka del dominio. */
object Topics {
    const val USER_EVENTS = "user-events"
    const val GAME_EVENTS = "game-events"
    const val LIBRARY_EVENTS = "library-events"
}

/** Tipos de evento del catálogo de juegos (topic [Topics.GAME_EVENTS]). */
object GameEventTypes {
    const val CREATED = "game.created"
    const val UPDATED = "game.updated"
    const val DELETED = "game.deleted"
}

/** Tipos de evento de biblioteca (topic [Topics.LIBRARY_EVENTS]). */
object LibraryEventTypes {
    const val ADDED = "library.added"
    const val UPDATED = "library.updated"
    const val REMOVED = "library.removed"
}

/**
 * Envoltura común de los eventos publicados en Kafka. `type` identifica la
 * clase de evento (p. ej. "user.registered") y `payload` los datos relevantes,
 * omitiendo siempre información sensible.
 */
data class EventEnvelope(
    val type: String = "",
    val occurredAt: Instant = Instant.now(),
    val payload: Map<String, Any?> = emptyMap(),
) {
    companion object {
        fun of(type: String, payload: Map<String, Any?>): EventEnvelope =
            EventEnvelope(type = type, occurredAt = Instant.now(), payload = payload)
    }
}