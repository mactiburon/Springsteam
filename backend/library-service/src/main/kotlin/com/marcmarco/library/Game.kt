package com.marcmarco.library

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

/**
 * Proyección local del catálogo de juegos, mantenida consumiendo los eventos
 * `game.created` / `game.updated` / `game.deleted` de game-service. El `id`
 * coincide con el id del juego en el catálogo (asignado, no autogenerado).
 */
@Entity
@Table(name = "games")
class Game(
    @Id
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

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