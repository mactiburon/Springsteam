package com.marcmarco.library

import com.marcmarco.library.dto.LibraryAddRequest
import com.marcmarco.library.dto.LibraryResponse
import com.marcmarco.library.dto.LibraryUpdateRequest
import com.marcmarco.library.security.userId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/library")
class LibraryController(private val libraryService: LibraryService) {

    @PostMapping
    fun add(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: LibraryAddRequest,
    ): ResponseEntity<LibraryResponse> {
        val entry = libraryService.add(jwt.userId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(LibraryResponse.from(entry))
    }

    @GetMapping
    fun search(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false, defaultValue = "false") favorites: Boolean,
    ): List<LibraryResponse> =
        libraryService.search(jwt.userId(), name, favorites).map { LibraryResponse.from(it) }

    @GetMapping("/{gameId}")
    fun getEntry(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable gameId: Long,
    ): LibraryResponse = LibraryResponse.from(libraryService.getEntry(jwt.userId(), gameId))

    @PutMapping("/{gameId}")
    fun update(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable gameId: Long,
        @Valid @RequestBody request: LibraryUpdateRequest,
    ): LibraryResponse = LibraryResponse.from(libraryService.update(jwt.userId(), gameId, request))

    @DeleteMapping("/{gameId}")
    fun remove(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable gameId: Long,
    ): ResponseEntity<Void> {
        libraryService.remove(jwt.userId(), gameId)
        return ResponseEntity.noContent().build()
    }
}