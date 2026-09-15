package com.marcmarco.library

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

/**
 * Wishlist ("lo que quiero conseguir") del usuario. Separada de la biblioteca
 * ("lo que ya tengo"): añadir a la wishlist no requiere tener el juego.
 */
@Entity
@Table(
    name = "wishlist_entries",
    uniqueConstraints = [UniqueConstraint(name = "uk_wishlist_user_game", columnNames = ["user_id", "game_id"])],
)
class WishlistEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id")
    val userId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    val game: Game? = null,

    @Column(updatable = false)
    val addedAt: Instant = Instant.now(),
)