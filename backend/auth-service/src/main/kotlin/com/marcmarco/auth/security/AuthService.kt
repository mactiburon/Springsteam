package com.marcmarco.auth.security

import com.marcmarco.auth.dto.LoginRequest
import com.marcmarco.auth.dto.LoginResponse
import com.marcmarco.auth.dto.UserResponse
import com.marcmarco.auth.user.UserRepository
import com.marcmarco.shared.error.InvalidCredentialsException
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Valida credenciales y emite un JWT firmado (HS256) cuyo `sub` es el id del
 * usuario. Devuelve «Credenciales incorrectas» tanto si el usuario no existe
 * como si la contraseña falla, para no revelar qué dato era el erróneo.
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtEncoder: JwtEncoder,
    @Value("\${app.jwt.expiration}") private val expirationSeconds: Long,
) {

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByUsernameAndActiveTrue(request.identifier)
            ?: userRepository.findByEmailAndActiveTrue(request.identifier)
            ?: throw InvalidCredentialsException("Credenciales incorrectas")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException("Credenciales incorrectas")
        }

        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(user.id.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expirationSeconds))
            .claim("username", user.username)
            .build()

        val token = jwtEncoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims),
        ).tokenValue
        return LoginResponse(
            token = token,
            tokenType = "Bearer",
            expiresIn = expirationSeconds,
            user = UserResponse.from(user),
        )
    }
}