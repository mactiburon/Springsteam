package com.marcmarco.springbootdemo.security.dto

import com.marcmarco.springbootdemo.user.dto.UserResponse

data class LoginResponse(
    val token: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse,
)