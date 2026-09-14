package com.marcmarco.springbootdemo.nickname.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NicknameRequest(
    @field:NotBlank(message = "El mote es obligatorio")
    @field:Size(max = 50, message = "El mote no puede superar 50 caracteres")
    val nickname: String,
)