package com.marcmarco.springbootdemo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

// API stateless basada en JWT: el servidor no guarda sesion, cada peticion trae su token.
// - Publicas: registro (POST /api/users), login (POST /api/users/login) y las lecturas
//   abiertas del catalogo (GET /api/users/**, /api/games/**, /api/search/**).
// - Protegidas: todo lo demas, incluido el handshake WebSocket (/ws), que exige el JWT
//   en la cabecera Authorization.
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/users/**", "/api/games/**", "/api/search/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt { } }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
        return http.build()
    }
}