package com.marcmarco.springbootdemo.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateEmailRequest(
    @field:NotBlank(message = "El email es obligatorio")
    @field:Email(message = "El email no tiene un formato válido")
    val email: String,
)