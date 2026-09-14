package com.marcmarco.springbootdemo.game

import com.marcmarco.springbootdemo.game.dto.ImportResponse
import com.marcmarco.springbootdemo.game.rawg.RawgGame
import com.marcmarco.springbootdemo.game.rawg.RawgRestClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GameImportService(
    private val rawgRestClient: RawgRestClient,
    private val gameRepository: GameRepository,
) {

    @Transactional
    fun importGames(count: Int): ImportResponse {
        val rawGames = rawgRestClient.fetchGames(count)
        var imported = 0
        var skipped = 0
        rawGames.forEach { raw ->
            if (raw.name.isBlank()) {
                skipped++
                return@forEach
            }
            if (gameRepository.existsByNameIgnoreCase(raw.name)) {
                skipped++
                return@forEach
            }
            gameRepository.save(raw.toGame())
            imported++
        }
        return ImportResponse(imported, skipped)
    }

    private fun RawgGame.toGame(): Game {
        val releaseDate = released?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return Game(
            name = name.trim(),
            genre = genres.firstOrNull()?.name,
            releaseDate = releaseDate,
            developer = developers.firstOrNull()?.name,
            publisher = publishers.firstOrNull()?.name,
            cover = background_image,
        )
    }
}