package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.LoginRequest
import com.marcmarco.frontend.api.dto.RegisterRequest
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

class AuthApiTest {

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private fun authApi(engine: MockEngine): AuthApi {
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
        return AuthApi(client)
    }

    @Test
    fun `login exitoso devuelve el token y el usuario`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/users/login", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            respond(
                content = """
                    {"token":"abc123","tokenType":"Bearer","expiresIn":86400,
                     "user":{"id":1,"username":"marc","email":"marc@test.com","displayName":"Marc","createdAt":"2026-01-01T00:00:00Z"}}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = authApi(engine)

        val res = api.login(LoginRequest("marc", "secreto123"))

        assertEquals("abc123", res.token)
        assertEquals("Bearer", res.tokenType)
        assertEquals(86400L, res.expiresIn)
        assertEquals("marc", res.user.username)
    }

    @Test
    fun `login con credenciales incorrectas lanza ApiException 401 con el mensaje`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """
                    {"status":401,"error":"Unauthorized","message":"Credenciales incorrectas","timestamp":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = authApi(engine)

        val e = assertFailsWith<ApiException> { api.login(LoginRequest("marc", "mala")) }

        assertEquals(401, e.status)
        assertEquals("Credenciales incorrectas", e.message)
    }

    @Test
    fun `registro exitoso devuelve el usuario creado`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/users", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            respond(
                content = """
                    {"id":5,"username":"nuevo","email":"nuevo@test.com","createdAt":"2026-01-01T00:00:00Z"}
                """.trimIndent(),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = authApi(engine)

        val res = api.register(RegisterRequest("nuevo", "nuevo@test.com", "secreto123"))

        assertEquals(5, res.id)
        assertEquals("nuevo", res.username)
    }
}