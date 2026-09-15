package com.marcmarco.library.event

import com.marcmarco.shared.event.EventEnvelope
import com.marcmarco.shared.event.GameEventTypes
import com.marcmarco.shared.event.Topics
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Consume los eventos del catálogo (`game-events`) y los aplica a la proyección
 * local de juegos de library-service.
 */
@Component
@ConditionalOnProperty(name = ["app.events.enabled"], havingValue = "true", matchIfMissing = true)
class GameEventConsumer(
    private val gameProjectionService: GameProjectionService,
) {

    @KafkaListener(topics = [Topics.GAME_EVENTS], groupId = "library-service")
    fun onGameEvent(envelope: EventEnvelope) {
        gameProjectionService.handleGameEvent(envelope.type, envelope.payload)
    }
}