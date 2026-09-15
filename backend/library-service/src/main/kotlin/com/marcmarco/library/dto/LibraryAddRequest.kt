package com.marcmarco.library.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class LibraryAddRequest(
    @field:NotNull(message = "El gameId es obligatorio")
    @field:Positive(message = "El gameId debe ser un número positivo")
    val gameId: Long,

    @field:JsonProperty("isFavorite")
    val isFavorite: Boolean? = null,

    @field:DecimalMin(value = "0.0", message = "Las horas jugadas no pueden ser negativas")
    val hoursPlayed: Double? = null,
)