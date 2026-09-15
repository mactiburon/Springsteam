package com.marcmarco.library.event

import com.marcmarco.shared.event.EventEnvelope
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

interface LibraryEventPublisher {
    fun publish(topic: String, type: String, payload: Map<String, Any?>)
}

@Component
@ConditionalOnProperty(name = ["app.events.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaLibraryEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, EventEnvelope>,
) : LibraryEventPublisher {

    override fun publish(topic: String, type: String, payload: Map<String, Any?>) {
        kafkaTemplate.send(topic, EventEnvelope.of(type, payload))
    }
}