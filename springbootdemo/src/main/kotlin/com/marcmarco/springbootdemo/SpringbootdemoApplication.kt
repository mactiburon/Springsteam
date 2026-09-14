package com.marcmarco.springbootdemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringbootdemoApplication

fun main(args: Array<String>) {
	runApplication<SpringbootdemoApplication>(*args)
}
