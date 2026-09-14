package com.marcmarco.frontend.ui

import com.marcmarco.frontend.api.dto.UserResponse

data class Session(
    val token: String,
    val expiresIn: Long,
    val user: UserResponse,
)