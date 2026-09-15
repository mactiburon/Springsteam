package com.marcmarco.library

import com.marcmarco.library.dto.CategoryRequest
import com.marcmarco.library.dto.CategoryResponse
import com.marcmarco.library.dto.LibraryResponse
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class LibraryCategoryController(private val categoryService: LibraryCategoryService) {

    @PostMapping
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CategoryRequest,
    ): ResponseEntity<CategoryResponse> {
        val category = categoryService.create(jwt.userId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(category)
    }

    @GetMapping
    fun list(@AuthenticationPrincipal jwt: Jwt): List<CategoryResponse> =
        categoryService.list(jwt.userId())

    @PutMapping("/{id}")
    fun rename(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
        @Valid @RequestBody request: CategoryRequest,
    ): CategoryResponse = categoryService.rename(jwt.userId(), id, request)

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        categoryService.delete(jwt.userId(), id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{categoryId}/library/{gameId}")
    fun assignToLibraryEntry(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable categoryId: Long,
        @PathVariable gameId: Long,
    ): LibraryResponse = LibraryResponse.from(categoryService.assignToLibraryEntry(jwt.userId(), gameId, categoryId))

    @DeleteMapping("/{categoryId}/library/{gameId}")
    fun unassignFromLibraryEntry(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable categoryId: Long,
        @PathVariable gameId: Long,
    ): LibraryResponse = LibraryResponse.from(categoryService.unassignFromLibraryEntry(jwt.userId(), gameId, categoryId))
}