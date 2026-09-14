package com.marcmarco.springbootdemo.security

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Autentica las conexiones STOMP: en el frame CONNECT lee el encabezado `Authorization`,
 * valida el JWT con el [JwtDecoder] y asocia el principal a la sesión. Los frames
 * siguientes de esa sesión heredan el principal automáticamente, así los handlers
 * STOMP conocen siempre al usuario autenticado.
 */
@Component
class StompAuthChannelInterceptor(private val jwtDecoder: JwtDecoder) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command == StompCommand.CONNECT) {
            val token = accessor.getFirstNativeHeader("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()
                ?: throw AuthorizationDeniedException("Se requiere un token JWT en la cabecera Authorization")
            val jwt = jwtDecoder.decode(token)
            accessor.setUser(JwtAuthenticationToken(jwt))
        }
        return message
    }
}