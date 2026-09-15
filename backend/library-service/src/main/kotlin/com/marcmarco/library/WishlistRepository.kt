package com.marcmarco.library

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WishlistRepository : JpaRepository<WishlistEntry, Long> {

    fun existsByUserIdAndGameId(userId: Long, gameId: Long): Boolean

    fun findByUserIdAndGameId(userId: Long, gameId: Long): WishlistEntry?

    /**
     * Búsqueda tipo Steam: filtros parciales case-insensitive sobre el nombre,
     * género, desarrollador y editor del juego proyectado, más un ámbito sobre el
     * estado en biblioteca (ALL / OWNED / NOT_OWNED). Siempre ordena por fecha
     * añadida descendente; el orden por otros campos se aplica en memoria.
     */
    @Query(
        """
        SELECT w FROM WishlistEntry w
        WHERE w.userId = :userId
          AND (:name = '' OR LOWER(w.game.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:genre = '' OR LOWER(COALESCE(w.game.genre, '')) LIKE LOWER(CONCAT('%', :genre, '%')))
          AND (:developer = '' OR LOWER(COALESCE(w.game.developer, '')) LIKE LOWER(CONCAT('%', :developer, '%')))
          AND (:publisher = '' OR LOWER(COALESCE(w.game.publisher, '')) LIKE LOWER(CONCAT('%', :publisher, '%')))
          AND (:libraryScope = 'ALL'
               OR (:libraryScope = 'OWNED'
                   AND EXISTS (SELECT e FROM LibraryEntry e WHERE e.userId = :userId AND e.game = w.game))
               OR (:libraryScope = 'NOT_OWNED'
                   AND NOT EXISTS (SELECT e FROM LibraryEntry e WHERE e.userId = :userId AND e.game = w.game)))
        ORDER BY w.addedAt DESC
        """
    )
    fun searchWishlist(
        @Param("userId") userId: Long,
        @Param("name") name: String,
        @Param("genre") genre: String,
        @Param("developer") developer: String,
        @Param("publisher") publisher: String,
        @Param("libraryScope") libraryScope: String,
    ): List<WishlistEntry>
}