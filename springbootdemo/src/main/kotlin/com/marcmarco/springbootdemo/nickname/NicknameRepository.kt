package com.marcmarco.springbootdemo.nickname

import org.springframework.data.jpa.repository.JpaRepository

interface NicknameRepository : JpaRepository<Nickname, Long> {

    fun findByOwnerIdAndTargetId(ownerId: Long, targetId: Long): Nickname?

    fun findByOwnerId(ownerId: Long): List<Nickname>
}