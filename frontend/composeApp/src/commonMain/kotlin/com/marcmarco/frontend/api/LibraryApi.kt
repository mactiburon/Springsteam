package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.LibraryAddRequest
import com.marcmarco.frontend.api.dto.LibraryResponse
import com.marcmarco.frontend.api.dto.LibraryUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class LibraryApi(
    private val client: HttpClient = defaultHttpClient(),
) {

    suspend fun listLibrary(
        token: String,
        name: String? = null,
        favoritesOnly: Boolean = false,
    ): List<LibraryResponse> {
        val response = client.get("${ApiConfig.BASE_URL}/api/library") {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (!name.isNullOrBlank()) parameter("name", name)
            if (favoritesOnly) parameter("favorites", "true")
        }
        return response.bodyOrThrow()
    }

    suspend fun addToLibrary(token: String, request: LibraryAddRequest): LibraryResponse {
        val response = client.post("${ApiConfig.BASE_URL}/api/library") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.bodyOrThrow()
    }

    suspend fun getEntry(token: String, gameId: Long): LibraryResponse {
        val response = client.get("${ApiConfig.BASE_URL}/api/library/$gameId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return response.bodyOrThrow()
    }

    suspend fun updateEntry(
        token: String,
        gameId: Long,
        request: LibraryUpdateRequest,
    ): LibraryResponse {
        val response = client.put("${ApiConfig.BASE_URL}/api/library/$gameId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.bodyOrThrow()
    }

    suspend fun removeFromLibrary(token: String, gameId: Long) {
        val response = client.delete("${ApiConfig.BASE_URL}/api/library/$gameId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        response.bodyOrThrow<Unit>()
    }

    fun close() = client.close()
}