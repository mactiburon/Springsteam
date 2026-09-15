package com.marcmarco.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.marcmarco.auth", "com.marcmarco.shared"])
class AuthApplication

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}