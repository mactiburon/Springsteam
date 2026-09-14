package com.marcmarco.frontend.api

import com.marcmarco.frontend.api.dto.ErrorResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    if (!status.isSuccess()) {
        throw ApiException(status.value, errorMessage())
    }
    return body()
}

internal suspend fun HttpResponse.errorMessage(): String =
    runCatching { Json.decodeFromString<ErrorResponse>(bodyAsText()).message }
        .getOrNull() ?: "Error inesperado (HTTP ${status.value})"