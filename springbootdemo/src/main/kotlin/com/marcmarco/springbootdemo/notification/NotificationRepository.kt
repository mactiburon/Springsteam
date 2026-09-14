package com.marcmarco.springbootdemo.notification

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByRecipientIdOrderByCreatedAtDesc(recipientId: Long): List<Notification>

    fun findByRecipientIdAndReadIsFalse(recipientId: Long): List<Notification>

    fun findByIdAndRecipientId(id: Long, recipientId: Long): Optional<Notification>

    fun countByRecipientIdAndReadIsFalse(recipientId: Long): Long
}