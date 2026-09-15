package com.marcmarco.library

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.marcmarco.library", "com.marcmarco.shared"])
class LibraryApplication

fun main(args: Array<String>) {
    runApplication<LibraryApplication>(*args)
}