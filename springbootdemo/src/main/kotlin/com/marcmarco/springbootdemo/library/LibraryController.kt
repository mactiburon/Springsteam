package com.marcmarco.springbootdemo.library

import com.marcmarco.springbootdemo.library.dto.LibraryAddRequest
import com.marcmarco.springbootdemo.library.dto.LibraryUpdateRequest
import com.marcmarco.springbootdemo.library.dto.LibraryResponse
import jakarta.validation.Valid
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

@RestController
@RequestMapping("/api/library")
class LibraryController(private val libraryService: LibraryService) {

    @PostMapping
    fun add(@Valid @RequestBody request: LibraryAddRequest): ResponseEntity<LibraryResponse> {
        val entry = libraryService.add(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(LibraryResponse.from(entry))
    }

    @GetMapping
    fun search(
        @RequestParam userId: Long,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false, defaultValue = "false") favorites: Boolean,
    ): List<LibraryResponse> =
        libraryService.search(userId, name, favorites).map { LibraryResponse.from(it) }

    @GetMapping("/{gameId}")
    fun getEntry(
        @PathVariable gameId: Long,
        @RequestParam userId: Long,
    ): LibraryResponse = LibraryResponse.from(libraryService.getEntry(userId, gameId))

    @PutMapping("/{gameId}")
    fun update(
        @PathVariable gameId: Long,
        @RequestParam userId: Long,
        @Valid @RequestBody request: LibraryUpdateRequest,
    ): LibraryResponse = LibraryResponse.from(libraryService.update(userId, gameId, request))

    @DeleteMapping("/{gameId}")
    fun remove(
        @PathVariable gameId: Long,
        @RequestParam userId: Long,
    ): ResponseEntity<Void> {
        libraryService.remove(userId, gameId)
        return ResponseEntity.noContent().build()
    }
}