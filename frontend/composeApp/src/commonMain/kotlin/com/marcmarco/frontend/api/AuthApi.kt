package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.LoginRequest
import com.marcmarco.frontend.api.dto.LoginResponse
import com.marcmarco.frontend.api.dto.RegisterRequest
import com.marcmarco.frontend.api.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(
    private val client: HttpClient = defaultHttpClient(),
) {

    suspend fun register(req: RegisterRequest): UserResponse =
        post("/api/users", req)

    suspend fun login(req: LoginRequest): LoginResponse =
        post("/api/users/login", req)

    fun close() = client.close()

    private suspend inline fun <reified T> post(path: String, body: Any): T {
        val response = client.post("${ApiConfig.BASE_URL}$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.bodyOrThrow()
    }
}