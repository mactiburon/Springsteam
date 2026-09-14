package com.marcmarco.springbootdemo.library

import com.marcmarco.springbootdemo.game.Game
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
    name = "library_entries",
    uniqueConstraints = [UniqueConstraint(name = "uk_library_user_game", columnNames = ["user_id", "game_id"])],
)
class LibraryEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    val game: Game,

    var isFavorite: Boolean = false,

    @Column(name = "hours_played")
    var hoursPlayed: Double = 0.0,

    @Column(updatable = false)
    val addedAt: Instant = Instant.now(),
)