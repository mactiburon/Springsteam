package com.marcmarco.library.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin

data class LibraryUpdateRequest(
    @field:JsonProperty("isFavorite")
    val isFavorite: Boolean? = null,

    @field:DecimalMin(value = "0.0", message = "Las horas jugadas no pueden ser negativas")
    val hoursPlayed: Double? = null,
)