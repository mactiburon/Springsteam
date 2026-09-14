package com.marcmarco.springbootdemo.library.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class LibraryAddRequest(
    @field:NotNull(message = "El userId es obligatorio")
    @field:Positive(message = "El userId debe ser un número positivo")
    val userId: Long,

    @field:NotNull(message = "El gameId es obligatorio")
    @field:Positive(message = "El gameId debe ser un número positivo")
    val gameId: Long,

    val isFavorite: Boolean? = null,

    @field:DecimalMin(value = "0.0", message = "Las horas jugadas no pueden ser negativas")
    val hoursPlayed: Double? = null,
)