package com.marcmarco.frontend.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameApiTest {

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private fun gameApi(engine: MockEngine): GameApi {
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return GameApi(client)
    }

    @Test
    fun `listGames devuelve el catalogo de juegos`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/games", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            assertTrue(request.url.parameters.isEmpty())
            respond(
                content = """
                    [{"id":1,"name":"Halo","genre":"Shooter","developer":"343","createdAt":"2026-01-01T00:00:00Z"},
                     {"id":2,"name":"Zelda","genre":"Aventura","createdAt":"2026-01-01T00:00:00Z"}]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = gameApi(engine)

        val games = api.listGames("token123")

        assertEquals(2, games.size)
        assertEquals("Halo", games[0].name)
        assertEquals("Aventura", games[1].genre)
    }

    @Test
    fun `listGames con filtros envia los parametros`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/games", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("halo", request.url.parameters["name"])
            assertEquals("Shooter", request.url.parameters["genre"])
            assertEquals("2020-01-01", request.url.parameters["releaseDateFrom"])
            respond(
                content = """
                    [{"id":1,"name":"Halo","genre":"Shooter","createdAt":"2026-01-01T00:00:00Z"}]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = gameApi(engine)

        val games = api.listGames(
            token = "token123",
            name = "halo",
            genre = "Shooter",
            releaseDateFrom = "2020-01-01",
        )

        assertEquals(1, games.size)
        assertEquals("Halo", games[0].name)
    }

    @Test
    fun `getGame devuelve el detalle del juego`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/games/3", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            respond(
                content = """
                    {"id":3,"name":"Mario Kart","description":"Carreras","genre":"Carreras","releaseDate":"2017-04-28","developer":"Nintendo","publisher":"Nintendo","createdAt":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = gameApi(engine)

        val game = api.getGame(3, "token123")

        assertEquals("Mario Kart", game.name)
        assertEquals("2017-04-28", game.releaseDate)
        assertEquals("Nintendo", game.developer)
    }

    @Test
    fun `getGame con juego inexistente lanza ApiException 404`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """
                    {"status":404,"error":"Not Found","message":"Juego no encontrado","timestamp":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = gameApi(engine)

        val e = assertFailsWith<ApiException> { api.getGame(999, "token123") }

        assertEquals(404, e.status)
        assertEquals("Juego no encontrado", e.message)
    }
}