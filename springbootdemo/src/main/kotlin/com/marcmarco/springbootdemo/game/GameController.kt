package com.marcmarco.springbootdemo.game

import com.marcmarco.springbootdemo.common.exception.BadRequestException
import com.marcmarco.springbootdemo.game.dto.GameRequest
import com.marcmarco.springbootdemo.game.dto.GameResponse
import com.marcmarco.springbootdemo.game.dto.ImportResponse
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/games")
class GameController(
    private val gameService: GameService,
    private val gameImportService: GameImportService,
) {

    @PostMapping("/import")
    fun importGames(
        @RequestParam(defaultValue = "40") count: Int,
    ): ImportResponse {
        if (count !in 1..100) {
            throw BadRequestException("count debe estar entre 1 y 100")
        }
        return gameImportService.importGames(count)
    }

    @PostMapping
    fun create(@Valid @RequestBody request: GameRequest): ResponseEntity<GameResponse> {
        val game = gameService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(GameResponse.from(game))
    }

    @GetMapping("/{id}")
    fun getGame(@PathVariable id: Long): GameResponse =
        GameResponse.from(gameService.getGame(id))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: GameRequest,
    ): GameResponse = GameResponse.from(gameService.update(id, request))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        gameService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun search(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) genre: String?,
        @RequestParam(required = false) developer: String?,
        @RequestParam(required = false) publisher: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        releaseDateFrom: LocalDate?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        releaseDateTo: LocalDate?,
    ): List<GameResponse> =
        gameService.search(name, genre, developer, publisher, releaseDateFrom, releaseDateTo)
            .map { GameResponse.from(it) }
}