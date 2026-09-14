package com.marcmarco.springbootdemo.notification

import com.marcmarco.springbootdemo.notification.dto.NotificationResponse
import com.marcmarco.springbootdemo.security.userId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val notificationService: NotificationService) {

    @GetMapping
    fun list(@AuthenticationPrincipal jwt: Jwt): List<NotificationResponse> =
        notificationService.list(jwt.userId()).map { NotificationResponse.from(it) }

    @GetMapping("/unread-count")
    fun unreadCount(@AuthenticationPrincipal jwt: Jwt): Map<String, Long> =
        mapOf("count" to notificationService.unreadCount(jwt.userId()))

    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
    ): NotificationResponse = NotificationResponse.from(notificationService.get(id, jwt.userId()))

    @PutMapping("/{id}/read")
    fun markRead(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
    ): NotificationResponse = NotificationResponse.from(notificationService.markRead(id, jwt.userId()))

    @PutMapping("/read-all")
    fun markAllRead(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Void> {
        notificationService.markAllRead(jwt.userId())
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        notificationService.delete(id, jwt.userId())
        return ResponseEntity.noContent().build()
    }
}