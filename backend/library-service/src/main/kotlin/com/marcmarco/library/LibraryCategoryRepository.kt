package com.marcmarco.library

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LibraryCategoryRepository : JpaRepository<LibraryCategory, Long> {

    fun findAllByUserIdOrderByName(userId: Long): List<LibraryCategory>

    fun findByUserIdAndId(userId: Long, id: Long): LibraryCategory?

    fun existsByUserIdAndNameIgnoreCase(userId: Long, name: String): Boolean

    fun existsByUserIdAndNameIgnoreCaseAndIdNot(userId: Long, name: String, id: Long): Boolean

    @Query(
        """
        SELECT c.id AS categoryId, COUNT(le) AS gameCount
        FROM LibraryEntry le JOIN le.categories c
        WHERE c.userId = :userId
        GROUP BY c.id
        """
    )
    fun countGamesByCategory(@Param("userId") userId: Long): List<CategoryGameCount>

    interface CategoryGameCount {
        val categoryId: Long
        val gameCount: Long
    }
}