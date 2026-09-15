package com.marcmarco.library.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.marcmarco.library.LibraryStatus
import jakarta.validation.constraints.DecimalMin
import java.time.Instant

data class LibraryUpdateRequest(
    @field:JsonProperty("isFavorite")
    val isFavorite: Boolean? = null,

    @field:DecimalMin(value = "0.0", message = "Las horas jugadas no pueden ser negativas")
    val hoursPlayed: Double? = null,

    val status: LibraryStatus? = null,

    val lastPlayedAt: Instant? = null,
)