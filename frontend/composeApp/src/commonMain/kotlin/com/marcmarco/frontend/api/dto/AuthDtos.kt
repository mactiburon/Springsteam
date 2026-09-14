package com.marcmarco.frontend.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String? = null,
)

@Serializable
data class LoginRequest(
    val identifier: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val displayName: String? = null,
    val bio: String? = null,
    val avatar: String? = null,
    val createdAt: String,
)

@Serializable
data class ErrorResponse(
    val status: Int,
    val error: String? = null,
    val message: String,
    val timestamp: String? = null,
)