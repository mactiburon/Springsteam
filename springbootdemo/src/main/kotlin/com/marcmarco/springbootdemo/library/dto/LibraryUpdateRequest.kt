package com.marcmarco.springbootdemo.library.dto

import jakarta.validation.constraints.DecimalMin

data class LibraryUpdateRequest(
    val isFavorite: Boolean? = null,

    @field:DecimalMin(value = "0.0", message = "Las horas jugadas no pueden ser negativas")
    val hoursPlayed: Double? = null,
)