package com.marcmarco.auth.dto

import jakarta.validation.constraints.NotBlank

data class DeleteAccountRequest(
    @field:NotBlank(message = "Debes indicar tu contraseña")
    val password: String,
)