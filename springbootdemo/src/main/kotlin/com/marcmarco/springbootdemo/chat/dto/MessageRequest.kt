package com.marcmarco.springbootdemo.chat.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MessageRequest(
    @field:NotBlank(message = "El mensaje no puede estar vacío")
    @field:Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres")
    val content: String,
)