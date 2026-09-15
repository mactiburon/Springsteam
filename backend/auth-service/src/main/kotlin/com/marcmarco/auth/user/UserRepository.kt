package com.marcmarco.auth.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {

    fun findByUsernameAndActiveTrue(username: String): User?

    fun findByEmailAndActiveTrue(email: String): User?

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun findByUsernameContainingIgnoreCaseAndActiveTrue(username: String): List<User>

    fun findAllByActiveTrue(): List<User>

    @Query(
        """
        SELECT u FROM User u
        WHERE u.active = true
          AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY u.username
        """
    )
    fun searchGlobal(@Param("q") q: String): List<User>
}