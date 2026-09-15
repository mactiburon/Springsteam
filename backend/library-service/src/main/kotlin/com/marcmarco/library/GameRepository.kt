package com.marcmarco.library

import org.springframework.data.jpa.repository.JpaRepository

interface GameRepository : JpaRepository<Game, Long>