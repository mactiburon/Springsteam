package com.marcmarco.springbootdemo.security

import org.springframework.security.oauth2.jwt.Jwt

/**
 * En este protocolo el JWT lleva el id del usuario como `sub` (subject).
 * Esta extensión evita repetir la conversión en cada controlador protegido.
 */
fun Jwt.userId(): Long =
    subject?.toLongOrNull()
        ?: throw IllegalStateException("El JWT autenticado no contiene un subject numérico válido")