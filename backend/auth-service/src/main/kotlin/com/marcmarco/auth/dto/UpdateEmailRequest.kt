package com.marcmarco.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateEmailRequest(
    @field:NotBlank(message = "Debes indicar el email")
    @field:Email(message = "El email no tiene un formato válido")
    val email: String,
)