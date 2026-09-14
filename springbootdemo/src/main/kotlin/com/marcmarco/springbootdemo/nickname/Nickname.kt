package com.marcmarco.springbootdemo.nickname

import com.marcmarco.springbootdemo.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "nicknames",
    uniqueConstraints = [UniqueConstraint(name = "uk_nickname_owner_target", columnNames = ["owner_id", "target_id"])],
)
class Nickname(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    val owner: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id")
    val target: User,

    @Column(nullable = false)
    var nickname: String,

    @Column(updatable = false)
    val createdAt: Instant = Instant.now(),

    var updatedAt: Instant = Instant.now(),
)