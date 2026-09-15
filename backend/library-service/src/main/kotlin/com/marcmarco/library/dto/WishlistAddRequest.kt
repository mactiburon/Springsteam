package com.marcmarco.library.dto

import jakarta.validation.constraints.NotNull

data class WishlistAddRequest(
    @field:NotNull(message = "El campo gameId es obligatorio")
    val gameId: Long? = null,
)