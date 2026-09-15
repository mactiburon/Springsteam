package com.marcmarco.auth.security

import org.springframework.security.oauth2.jwt.Jwt

/** El `sub` del JWT es el id de usuario; evita repetir la conversión. */
fun Jwt.userId(): Long =
    subject?.toLongOrNull()
        ?: throw IllegalStateException("El JWT autenticado no contiene un subject numérico válido")

fun Jwt.userIdOrFallback(fallback: Long = 0L): Long = subject?.toLongOrNull() ?: fallback