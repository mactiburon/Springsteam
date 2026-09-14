package com.marcmarco.springbootdemo.friendship.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class FriendshipRequest(
    @field:NotNull(message = "El addresseeId es obligatorio")
    @field:Positive(message = "El addresseeId debe ser un número positivo")
    val addresseeId: Long,
)