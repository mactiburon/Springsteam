package com.marcmarco.frontend.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class LibraryAddRequest(
    val gameId: Long,
    val isFavorite: Boolean? = null,
    val hoursPlayed: Double? = null,
)

@Serializable
data class LibraryUpdateRequest(
    val isFavorite: Boolean? = null,
    val hoursPlayed: Double? = null,
)

@Serializable
data class LibraryResponse(
    val userId: Long,
    val gameId: Long,
    val game: GameResponse,
    val isFavorite: Boolean,
    val hoursPlayed: Double,
    val addedAt: String,
)