package com.marcmarco.springbootdemo.chat

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface MessageRepository : JpaRepository<Message, Long> {

    fun findByConversationIdOrderBySentAtAsc(conversationId: Long, pageable: Pageable): List<Message>

    fun findTopByConversationIdOrderBySentAtDesc(conversationId: Long): Message?
}