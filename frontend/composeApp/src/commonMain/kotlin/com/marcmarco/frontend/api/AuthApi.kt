package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.ErrorResponse
import com.marcmarco.frontend.api.dto.LoginRequest
import com.marcmarco.frontend.api.dto.LoginResponse
import com.marcmarco.frontend.api.dto.RegisterRequest
import com.marcmarco.frontend.api.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AuthApi(
    private val client: HttpClient = defaultHttpClient(),
) {

    suspend fun register(req: RegisterRequest): UserResponse =
        post("/api/users", req)

    suspend fun login(req: LoginRequest): LoginResponse =
        post("/api/users/login", req)

    suspend fun getUser(id: Long, token: String): UserResponse {
        val response = client.get("${ApiConfig.BASE_URL}/api/users/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return response.toResult()
    }

    fun close() = client.close()

    private suspend inline fun <reified T> post(path: String, body: Any): T {
        val response = client.post("${ApiConfig.BASE_URL}$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.toResult()
    }

    private suspend inline fun <reified T> HttpResponse.toResult(): T {
        if (!status.isSuccess()) {
            throw ApiException(status.value, errorMessage())
        }
        return body()
    }

    private suspend fun HttpResponse.errorMessage(): String =
        runCatching { Json.decodeFromString<ErrorResponse>(bodyAsText()).message }
            .getOrNull() ?: "Error inesperado (HTTP ${status.value})"
}