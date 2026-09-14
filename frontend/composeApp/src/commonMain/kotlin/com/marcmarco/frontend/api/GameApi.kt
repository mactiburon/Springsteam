package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.GameResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders

class GameApi(
    private val client: HttpClient = defaultHttpClient(),
) {

    suspend fun listGames(
        token: String,
        name: String? = null,
        genre: String? = null,
        developer: String? = null,
        publisher: String? = null,
        releaseDateFrom: String? = null,
        releaseDateTo: String? = null,
    ): List<GameResponse> {
        val response = client.get("${ApiConfig.BASE_URL}/api/games") {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (!name.isNullOrBlank()) parameter("name", name)
            if (!genre.isNullOrBlank()) parameter("genre", genre)
            if (!developer.isNullOrBlank()) parameter("developer", developer)
            if (!publisher.isNullOrBlank()) parameter("publisher", publisher)
            if (!releaseDateFrom.isNullOrBlank()) parameter("releaseDateFrom", releaseDateFrom)
            if (!releaseDateTo.isNullOrBlank()) parameter("releaseDateTo", releaseDateTo)
        }
        return response.bodyOrThrow()
    }

    suspend fun getGame(id: Long, token: String): GameResponse {
        val response = client.get("${ApiConfig.BASE_URL}/api/games/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return response.bodyOrThrow()
    }

    fun close() = client.close()
}