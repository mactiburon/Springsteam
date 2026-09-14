package com.marcmarco.springbootdemo.chat.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class ConversationRequest(
    @field:NotNull(message = "El initiatorId es obligatorio")
    @field:Positive(message = "El initiatorId debe ser un número positivo")
    val initiatorId: Long,

    @field:NotNull(message = "El participantId es obligatorio")
    @field:Positive(message = "El participantId debe ser un número positivo")
    val participantId: Long,
)