package com.marcmarco.game

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.marcmarco.game", "com.marcmarco.shared"])
class GameApplication

fun main(args: Array<String>) {
    runApplication<GameApplication>(*args)
}