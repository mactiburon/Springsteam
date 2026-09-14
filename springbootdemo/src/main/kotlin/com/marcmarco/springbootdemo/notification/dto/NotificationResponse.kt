package com.marcmarco.springbootdemo.notification.dto

import com.marcmarco.springbootdemo.notification.Notification
import com.marcmarco.springbootdemo.notification.NotificationType
import com.marcmarco.springbootdemo.user.dto.UserResponse
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val message: String,
    val referenceId: Long?,
    val read: Boolean,
    val actor: UserResponse,
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse = NotificationResponse(
            id = requireNotNull(notification.id),
            type = notification.type,
            message = notification.message,
            referenceId = notification.referenceId,
            read = notification.read,
            actor = UserResponse.from(notification.actor),
            createdAt = notification.createdAt,
        )
    }
}