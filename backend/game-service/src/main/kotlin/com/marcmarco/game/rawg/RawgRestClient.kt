package com.marcmarco.game.rawg

import com.fasterxml.jackson.databind.ObjectMapper
import com.marcmarco.shared.error.BadRequestException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RawgRestClient(
    private val objectMapper: ObjectMapper,
    @Value("\${app.rawg.api-key}") private val apiKey: String,
    @Value("\${app.rawg.base-url}") private val baseUrl: String,
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun fetchGames(count: Int): List<RawgGame> {
        if (apiKey.isBlank()) {
            throw BadRequestException("RAWG_API_KEY no está configurada en .env")
        }
        val pageSize = 40
        val pages = (count + pageSize - 1) / pageSize
        val games = mutableListOf<RawgGame>()
        for (page in 1..pages) {
            val json = restClient.get()
                .uri { it.path("/games").queryParam("key", apiKey).queryParam("page_size", pageSize).queryParam("page", page).build() }
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError) { _, httpResponse ->
                    throw BadRequestException("RAWG respondió ${httpResponse.statusCode.value()}")
                }
                .body(String::class.java)
            val response = objectMapper.readValue(json ?: "{}", RawgListResponse::class.java)
            games += response.results
        }
        return games.take(count)
    }
}