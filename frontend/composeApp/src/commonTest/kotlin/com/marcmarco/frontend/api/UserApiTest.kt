package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.UpdateProfileRequest
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

class UserApiTest {

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private fun userApi(engine: MockEngine): UserApi {
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return UserApi(client)
    }

    @Test
    fun `listUsers devuelve la lista de usuarios`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/users", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            assertTrue(request.url.parameters.isEmpty())
            respond(
                content = """
                    [{"id":1,"username":"marc","email":"marc@test.com","createdAt":"2026-01-01T00:00:00Z"},
                     {"id":2,"username":"ana","email":"ana@test.com","createdAt":"2026-01-01T00:00:00Z"}]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = userApi(engine)

        val users = api.listUsers("token123")

        assertEquals(2, users.size)
        assertEquals("marc", users[0].username)
        assertEquals("ana", users[1].username)
    }

    @Test
    fun `listUsers con username envia el parametro de busqueda`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/users", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("marc", request.url.parameters["username"])
            respond(
                content = """
                    [{"id":1,"username":"marc","email":"marc@test.com","createdAt":"2026-01-01T00:00:00Z"}]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = userApi(engine)

        val users = api.listUsers("token123", username = "marc")

        assertEquals(1, users.size)
        assertEquals("marc", users[0].username)
    }

    @Test
    fun `getUser devuelve el usuario`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/users/7", request.url.encodedPath)
            assertEquals(HttpMethod.Get, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            respond(
                content = """
                    {"id":7,"username":"ana","email":"ana@test.com","displayName":"Ana","createdAt":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = userApi(engine)

        val user = api.getUser(7, "token123")

        assertEquals(7, user.id)
        assertEquals("Ana", user.displayName)
    }

    @Test
    fun `getUser con usuario inexistente lanza ApiException 404`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """
                    {"status":404,"error":"Not Found","message":"Usuario no encontrado","timestamp":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = userApi(engine)

        val e = assertFailsWith<ApiException> { api.getUser(999, "token123") }

        assertEquals(404, e.status)
        assertEquals("Usuario no encontrado", e.message)
    }

    @Test
    fun `updateProfile actualiza y devuelve el usuario`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/users/me", request.url.encodedPath)
            assertEquals(HttpMethod.Put, request.method)
            check(request.headers[HttpHeaders.Authorization] == "Bearer token123")
            respond(
                content = """
                    {"id":1,"username":"marc","email":"marc@test.com","displayName":"Marc Navarro","bio":"Dev","createdAt":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = userApi(engine)

        val updated = api.updateProfile(
            token = "token123",
            request = UpdateProfileRequest(displayName = "Marc Navarro", bio = "Dev"),
        )

        assertEquals("Marc Navarro", updated.displayName)
        assertEquals("Dev", updated.bio)
    }

    @Test
    fun `updateProfile con datos invalidos lanza ApiException 400`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """
                    {"status":400,"error":"Bad Request","message":"El nombre visible debe tener entre 3 y 30 caracteres","timestamp":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = userApi(engine)

        val e = assertFailsWith<ApiException> {
            api.updateProfile("token123", UpdateProfileRequest(displayName = "A"))
        }

        assertEquals(400, e.status)
        assertEquals("El nombre visible debe tener entre 3 y 30 caracteres", e.message)
    }
}