package com.marcmarco.auth.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Debes indicar tu username o email")
    val identifier: String,

    @field:NotBlank(message = "Debes indicar tu contraseña")
    val password: String,
)