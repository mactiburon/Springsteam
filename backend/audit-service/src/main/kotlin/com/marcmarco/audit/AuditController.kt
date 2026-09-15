package com.marcmarco.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.marcmarco.audit.dto.AuditResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/audit")
class AuditController(
    private val auditService: AuditService,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping
    fun search(
        @RequestParam(required = false) topic: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) gameId: Long?,
        @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(1000) limit: Int,
    ): List<AuditResponse> =
        auditService.search(topic, type, userId, gameId, limit)
            .map { AuditResponse.from(it, objectMapper) }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): AuditResponse =
        AuditResponse.from(auditService.get(id), objectMapper)
}