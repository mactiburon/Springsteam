package com.marcmarco.springbootdemo.chat

import com.marcmarco.springbootdemo.security.StompAuthChannelInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.JacksonJsonMessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val objectMapper: ObjectMapper,
    private val stompAuthChannelInterceptor: StompAuthChannelInterceptor,
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompAuthChannelInterceptor)
    }

    override fun configureMessageConverters(messageConverters: MutableList<MessageConverter>): Boolean {
        messageConverters.add(JacksonJsonMessageConverter(objectMapper as JsonMapper))
        return true
    }
}