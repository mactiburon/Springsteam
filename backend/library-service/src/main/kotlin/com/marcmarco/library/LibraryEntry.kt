package com.marcmarco.library

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
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

    @Column(name = "user_id")
    val userId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    val game: Game? = null,

    var isFavorite: Boolean = false,

    @Column(name = "hours_played")
    var hoursPlayed: Double = 0.0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: LibraryStatus = LibraryStatus.PENDING,

    @Column(name = "last_played_at")
    var lastPlayedAt: Instant? = null,

    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    @JoinTable(
        name = "library_entry_categories",
        joinColumns = [JoinColumn(name = "library_entry_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")],
    )
    var categories: MutableSet<LibraryCategory> = mutableSetOf(),

    @Column(updatable = false)
    val addedAt: Instant = Instant.now(),
)