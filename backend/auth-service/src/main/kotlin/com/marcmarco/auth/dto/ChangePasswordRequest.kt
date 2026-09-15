package com.marcmarco.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "Debes indicar tu contraseña actual")
    val currentPassword: String,

    @field:NotBlank(message = "Debes indicar la nueva contraseña")
    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val newPassword: String,
)