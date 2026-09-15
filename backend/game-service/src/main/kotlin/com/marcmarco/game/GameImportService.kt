package com.marcmarco.game

import com.marcmarco.game.dto.ImportResponse
import com.marcmarco.game.event.GameEventPublisher
import com.marcmarco.game.rawg.RawgGame
import com.marcmarco.game.rawg.RawgRestClient
import com.marcmarco.shared.event.GameEventTypes
import com.marcmarco.shared.event.Topics
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GameImportService(
    private val rawgRestClient: RawgRestClient,
    private val gameRepository: GameRepository,
    @Autowired(required = false) private val eventPublisher: GameEventPublisher? = null,
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
            val game = gameRepository.save(raw.toGame())
            eventPublisher?.publish(
                topic = Topics.GAME_EVENTS,
                type = GameEventTypes.CREATED,
                payload = game.toEventPayload(),
            )
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