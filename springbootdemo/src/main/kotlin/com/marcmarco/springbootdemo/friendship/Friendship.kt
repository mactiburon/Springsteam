package com.marcmarco.springbootdemo.friendship

import com.marcmarco.springbootdemo.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    name = "friendships",
    uniqueConstraints = [UniqueConstraint(name = "uk_friendship_pair", columnNames = ["requester_id", "addressee_id"])],
)
class Friendship(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    val requester: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id")
    val addressee: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FriendshipStatus = FriendshipStatus.PENDING,

    @Column(updatable = false)
    val createdAt: Instant = Instant.now(),

    var updatedAt: Instant = Instant.now(),
)