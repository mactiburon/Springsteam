package com.marcmarco.shared.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.util.Base64URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Contrato común de seguridad para los microservicios: una fuente única de
 * clave HS256, decodificador y encodificador JWT a partir de `app.jwt.secret`.
 * Todos los servicios comparten el mismo secreto para validar los tokens que
 * emite auth-service.
 */
@Configuration(proxyBeanMethods = false)
class JwtSecurityConfig {

    @Bean
    fun jwtSecret(@Value("\${app.jwt.secret}") secret: String): SecretKey =
        SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")

    @Bean
    fun jwtDecoder(jwtSecret: SecretKey): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(jwtSecret).build()

    @Bean
    fun jwtEncoder(jwtSecret: SecretKey): JwtEncoder {
        val jwk = OctetSequenceKey.Builder(Base64URL.encode(jwtSecret.encoded))
            .algorithm(JWSAlgorithm.HS256)
            .build()
        return NimbusJwtEncoder(ImmutableJWKSet(JWKSet(jwk)))
    }
}