package com.marcmarco.springbootdemo.search.dto

import com.marcmarco.springbootdemo.game.dto.GameResponse
import com.marcmarco.springbootdemo.user.dto.UserResponse

data class SearchResponse(
    val games: List<GameResponse>,
    val users: List<UserResponse>,
)