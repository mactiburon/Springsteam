package com.marcmarco.auth.dto

data class LoginResponse(
    val token: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse,
)