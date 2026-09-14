package com.marcmarco.frontend.api

class ApiException(
    val status: Int,
    override val message: String,
) : Exception(message)