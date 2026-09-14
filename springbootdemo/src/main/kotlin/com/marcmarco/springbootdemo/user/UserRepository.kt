package com.marcmarco.springbootdemo.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {

    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun findByUsernameContainingIgnoreCase(username: String): List<User>

    @Query(
        """
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY u.username
        """
    )
    fun searchGlobal(@Param("q") q: String): List<User>
}