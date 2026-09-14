package com.marcmarco.springbootdemo.game.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class GameRequest(
    @field:NotBlank(message = "El nombre del juego es obligatorio")
    @field:Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    val name: String,

    @field:Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
    val description: String? = null,

    @field:Size(max = 100, message = "El género no puede superar 100 caracteres")
    val genre: String? = null,

    val releaseDate: LocalDate? = null,

    @field:Size(max = 200, message = "El desarrollador no puede superar 200 caracteres")
    val developer: String? = null,

    @field:Size(max = 200, message = "El publisher no puede superar 200 caracteres")
    val publisher: String? = null,

    @field:Size(max = 500, message = "La portada no puede superar 500 caracteres")
    val cover: String? = null,
)