package com.marcmarco.springbootdemo.notification

import com.marcmarco.springbootdemo.notification.dto.NotificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val notificationService: NotificationService) {

    @GetMapping
    fun list(@RequestParam userId: Long): List<NotificationResponse> =
        notificationService.list(userId).map { NotificationResponse.from(it) }

    @GetMapping("/unread-count")
    fun unreadCount(@RequestParam userId: Long): Map<String, Long> =
        mapOf("count" to notificationService.unreadCount(userId))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, @RequestParam userId: Long): NotificationResponse =
        NotificationResponse.from(notificationService.get(id, userId))

    @PutMapping("/{id}/read")
    fun markRead(@PathVariable id: Long, @RequestParam userId: Long): NotificationResponse =
        NotificationResponse.from(notificationService.markRead(id, userId))

    @PutMapping("/read-all")
    fun markAllRead(@RequestParam userId: Long): ResponseEntity<Void> {
        notificationService.markAllRead(userId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, @RequestParam userId: Long): ResponseEntity<Void> {
        notificationService.delete(id, userId)
        return ResponseEntity.noContent().build()
    }
}