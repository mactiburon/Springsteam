package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.LibraryAddRequest
import com.marcmarco.frontend.api.dto.LibraryUpdateRequest
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

class LibraryApiTest {

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private fun libraryApi(engine: MockEngine): LibraryApi {
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return LibraryApi(client)
    }

    private val libraryEntryJson = """
        {"userId":1,"gameId":1,"game":{"id":1,"name":"Halo","genre":"Shooter","developer":"343","createdAt":"2026-01-01T00:00:00Z"},"isFavorite":true,"hoursPlayed":12.5,"addedAt":"2026-01-01T00:00:00Z"}
    """.trimIndent()

    @Test
    fun `listLibrary devuelve las entradas de la biblioteca`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/library", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            assertTrue(request.url.parameters.isEmpty())
            respond(
                content = "[$libraryEntryJson]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = libraryApi(engine)

        val entries = api.listLibrary("token123")

        assertEquals(1, entries.size)
        assertEquals("Halo", entries[0].game.name)
        assertTrue(entries[0].isFavorite)
        assertEquals(12.5, entries[0].hoursPlayed)
    }

    @Test
    fun `listLibrary con nombre y favoritos envia los parametros`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/library", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("halo", request.url.parameters["name"])
            assertEquals("true", request.url.parameters["favorites"])
            respond(
                content = "[$libraryEntryJson]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = libraryApi(engine)

        val entries = api.listLibrary(token = "token123", name = "halo", favoritesOnly = true)

        assertEquals(1, entries.size)
    }

    @Test
    fun `addToLibrary envia POST con el body y devuelve la entrada`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/library", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            respond(
                content = libraryEntryJson,
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = libraryApi(engine)

        val entry = api.addToLibrary("token123", LibraryAddRequest(gameId = 1, isFavorite = true))

        assertEquals(1, entry.gameId)
        assertEquals("Halo", entry.game.name)
    }

    @Test
    fun `updateEntry envia PUT y devuelve la entrada actualizada`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/library/1", request.url.encodedPath)
            assertEquals(HttpMethod.Put, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            respond(
                content = libraryEntryJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = libraryApi(engine)

        val updated = api.updateEntry("token123", 1, LibraryUpdateRequest(hoursPlayed = 20.0))

        assertEquals(1, updated.gameId)
        assertTrue(updated.isFavorite)
    }

    @Test
    fun `removeFromLibrary con 204 no lanza`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/library/1", request.url.encodedPath)
            assertEquals(HttpMethod.Delete, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        val api = libraryApi(engine)

        api.removeFromLibrary("token123", 1)
    }

    @Test
    fun `removeFromLibrary con juego no en biblioteca lanza ApiException 404`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """
                    {"status":404,"error":"Not Found","message":"El usuario 1 no tiene el juego 999 en su biblioteca","timestamp":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = libraryApi(engine)

        val e = assertFailsWith<ApiException> { api.removeFromLibrary("token123", 999) }

        assertEquals(404, e.status)
        assertTrue(e.message.orEmpty().contains("en su biblioteca"))
    }
}