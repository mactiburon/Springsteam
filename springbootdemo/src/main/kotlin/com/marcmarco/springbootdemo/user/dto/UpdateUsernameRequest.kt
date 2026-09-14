package com.marcmarco.springbootdemo.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateUsernameRequest(
    @field:NotBlank(message = "El username es obligatorio")
    @field:Size(min = 3, max = 30, message = "El username debe tener entre 3 y 30 caracteres")
    val username: String,
)