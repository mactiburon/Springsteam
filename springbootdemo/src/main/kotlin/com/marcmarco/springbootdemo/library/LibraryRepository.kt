package com.marcmarco.springbootdemo.library

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LibraryRepository : JpaRepository<LibraryEntry, Long> {

    fun findByUserIdAndGameId(userId: Long, gameId: Long): LibraryEntry?

    fun existsByUserIdAndGameId(userId: Long, gameId: Long): Boolean

    @Query(
        """
        SELECT le FROM LibraryEntry le
        WHERE le.user.id = :userId
          AND (:name IS NULL OR LOWER(le.game.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:onlyFavorites = false OR le.isFavorite = true)
        ORDER BY le.addedAt DESC
        """
    )
    fun search(
        @Param("userId") userId: Long,
        @Param("name") name: String?,
        @Param("onlyFavorites") onlyFavorites: Boolean,
    ): List<LibraryEntry>
}