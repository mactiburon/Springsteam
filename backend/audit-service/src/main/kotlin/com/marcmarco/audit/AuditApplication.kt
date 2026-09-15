package com.marcmarco.audit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.marcmarco.audit", "com.marcmarco.shared"])
class AuditApplication

fun main(args: Array<String>) {
    runApplication<AuditApplication>(*args)
}