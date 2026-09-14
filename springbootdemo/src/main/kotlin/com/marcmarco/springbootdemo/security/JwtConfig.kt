package com.marcmarco.springbootdemo.security

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
 * Configura el cifrado y la validación de los JWT (HS256) con una clave simétrica.
 * El mismo `SecretKey` se usa para ENTRAR (encoder) y SALIR (decoder), así que el
 * backend puede emitir y verificar sus propios tokens sin infraestructura externa.
 * El JWK del encoder declara explícitamente el algoritmo HS256 para que el selector
 * de clave del `NimbusJwtEncoder` lo encuentre de forma determinista.
 */
@Configuration
class JwtConfig {

    @Bean
    fun jwtSecret(@Value("\${app.jwt.secret}") secret: String): SecretKey =
        SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")

    @Bean
    fun jwtEncoder(jwtSecret: SecretKey): JwtEncoder {
        val jwk = OctetSequenceKey.Builder(Base64URL.encode(jwtSecret.encoded))
            .algorithm(JWSAlgorithm.HS256)
            .build()
        return NimbusJwtEncoder(ImmutableJWKSet(JWKSet(jwk)))
    }

    @Bean
    fun jwtDecoder(jwtSecret: SecretKey): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(jwtSecret).build()
}