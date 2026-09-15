package com.marcmarco.library.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CategoryRequest(
    @field:NotBlank(message = "El nombre de la categoría es obligatorio")
    @field:Size(max = 50, message = "El nombre no puede superar 50 caracteres")
    val name: String,
)