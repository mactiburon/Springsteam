package com.marcmarco.springbootdemo.search

import com.marcmarco.springbootdemo.game.GameRepository
import com.marcmarco.springbootdemo.game.dto.GameResponse
import com.marcmarco.springbootdemo.search.dto.SearchResponse
import com.marcmarco.springbootdemo.user.UserRepository
import com.marcmarco.springbootdemo.user.dto.UserResponse
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
) {

    fun globalSearch(query: String): SearchResponse {
        val q = query.trim()
        if (q.isEmpty()) {
            return SearchResponse(games = emptyList(), users = emptyList())
        }
        return SearchResponse(
            games = gameRepository.searchGlobal(q).map { GameResponse.from(it) },
            users = userRepository.searchGlobal(q).map { UserResponse.from(it) },
        )
    }

    fun suggestions(query: String, limit: Int = 5): List<String> {
        val q = query.trim()
        if (q.isEmpty()) {
            return emptyList()
        }
        val gameNames = gameRepository.searchGlobal(q).map { it.name }
        val usernames = userRepository.searchGlobal(q).map { it.username }
        return (gameNames + usernames).distinct().take(limit)
    }
}