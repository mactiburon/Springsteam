package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.UpdateProfileRequest
import com.marcmarco.frontend.api.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class UserApi(
    private val client: HttpClient = defaultHttpClient(),
) {

    suspend fun getUser(id: Long, token: String): UserResponse {
        val response = client.get("${ApiConfig.BASE_URL}/api/users/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return response.bodyOrThrow()
    }

    suspend fun listUsers(token: String, username: String? = null): List<UserResponse> {
        val response = client.get("${ApiConfig.BASE_URL}/api/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (!username.isNullOrBlank()) {
                parameter("username", username)
            }
        }
        return response.bodyOrThrow()
    }

    suspend fun updateProfile(token: String, request: UpdateProfileRequest): UserResponse {
        val response = client.put("${ApiConfig.BASE_URL}/api/users/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.bodyOrThrow()
    }

    fun close() = client.close()
}