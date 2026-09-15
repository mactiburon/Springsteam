package com.marcmarco.audit

import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import java.time.Instant

/**
 * Emite un JWT HS256 de prueba con `sub` = userId sin depender de auth-service.
 */
fun JwtEncoder.testToken(userId: Long = 1L): String {
    val now = Instant.now()
    return encode(
        JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build(),
        ),
    ).tokenValue
}