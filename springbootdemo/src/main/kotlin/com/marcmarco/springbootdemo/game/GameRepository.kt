package com.marcmarco.springbootdemo.game

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface GameRepository : JpaRepository<Game, Long> {

    fun existsByName(name: String): Boolean

    fun existsByNameIgnoreCase(name: String): Boolean

    @Query(
        """
        SELECT g FROM Game g
        WHERE (:name IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:genre IS NULL OR LOWER(g.genre) LIKE LOWER(CONCAT('%', :genre, '%')))
          AND (:developer IS NULL OR LOWER(g.developer) LIKE LOWER(CONCAT('%', :developer, '%')))
          AND (:publisher IS NULL OR LOWER(g.publisher) LIKE LOWER(CONCAT('%', :publisher, '%')))
          AND (:releaseDateFrom IS NULL OR g.releaseDate >= :releaseDateFrom)
          AND (:releaseDateTo IS NULL OR g.releaseDate <= :releaseDateTo)
        """
    )
    fun search(
        @Param("name") name: String?,
        @Param("genre") genre: String?,
        @Param("developer") developer: String?,
        @Param("publisher") publisher: String?,
        @Param("releaseDateFrom") releaseDateFrom: LocalDate?,
        @Param("releaseDateTo") releaseDateTo: LocalDate?,
    ): List<Game>

    @Query(
        """
        SELECT g FROM Game g
        WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(g.genre) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(g.developer) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(g.publisher) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY g.name
        """
    )
    fun searchGlobal(@Param("q") q: String): List<Game>
}