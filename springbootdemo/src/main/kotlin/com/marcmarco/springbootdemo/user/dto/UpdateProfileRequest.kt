package com.marcmarco.springbootdemo.user.dto

import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(min = 3, max = 30, message = "El nombre mostrado debe tener entre 3 y 30 caracteres")
    val displayName: String? = null,

    @field:Size(max = 500, message = "La bio no puede superar 500 caracteres")
    val bio: String? = null,

    @field:Size(max = 300, message = "El avatar no puede superar 300 caracteres")
    val avatar: String? = null,
)