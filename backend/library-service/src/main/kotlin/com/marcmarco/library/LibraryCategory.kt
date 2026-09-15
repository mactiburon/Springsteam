package com.marcmarco.library

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * Categoría personal creada por un usuario para organizar su propia biblioteca.
 * Es estrictamente por usuario: otro usuario no puede verla, usarla ni asignarla.
 * (No son géneros del catálogo, que viven en game-service.)
 */
@Entity
@Table(
    name = "library_categories",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_library_category_user_name", columnNames = ["user_id", "name"]),
    ],
)
class LibraryCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(nullable = false, length = 50)
    var name: String = "",

    @Column(updatable = false)
    val createdAt: Instant = Instant.now(),
)