package com.marcmarco.library.security

import org.springframework.security.oauth2.jwt.Jwt

/** El `sub` del JWT es el id de usuario; evita repetir la conversión. */
fun Jwt.userId(): Long =
    subject?.toLongOrNull()
        ?: throw IllegalStateException("El JWT autenticado no contiene un subject numérico válido")