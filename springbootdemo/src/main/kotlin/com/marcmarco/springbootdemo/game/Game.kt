package com.marcmarco.springbootdemo.game

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "games")
class Game(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String,

    var description: String? = null,

    var genre: String? = null,

    @Column(name = "release_date")
    var releaseDate: LocalDate? = null,

    var developer: String? = null,

    var publisher: String? = null,

    var cover: String? = null,

    @Column(updatable = false)
    val createdAt: Instant = Instant.now(),
)