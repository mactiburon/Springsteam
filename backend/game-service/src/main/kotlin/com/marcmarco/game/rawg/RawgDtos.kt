package com.marcmarco.game.rawg

data class RawgListResponse(
    val results: List<RawgGame> = emptyList(),
)

data class RawgGame(
    val name: String,
    val released: String? = null,
    val background_image: String? = null,
    val genres: List<RawgNamedItem> = emptyList(),
    val developers: List<RawgNamedItem> = emptyList(),
    val publishers: List<RawgNamedItem> = emptyList(),
)

data class RawgNamedItem(
    val name: String,
)