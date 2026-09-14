package com.marcmarco.springbootdemo.user.dto

import jakarta.validation.constraints.NotBlank

data class DeleteAccountRequest(
    @field:NotBlank(message = "La contraseña es obligatoria para eliminar la cuenta")
    val password: String,
)