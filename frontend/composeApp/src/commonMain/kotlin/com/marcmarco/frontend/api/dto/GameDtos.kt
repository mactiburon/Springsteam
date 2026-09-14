package com.marcmarco.frontend.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class GameResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val cover: String? = null,
    val createdAt: String,
)