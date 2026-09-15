package com.marcmarco.library

import com.marcmarco.library.dto.WishlistAddRequest
import com.marcmarco.library.dto.WishlistResponse
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/wishlist")
class WishlistController(private val wishlistService: WishlistService) {

    @PostMapping
    fun add(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: WishlistAddRequest,
    ): ResponseEntity<WishlistResponse> {
        val entry = wishlistService.add(jwt.userId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(WishlistResponse.from(entry))
    }

    @GetMapping
    fun search(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) genre: String?,
        @RequestParam(required = false) developer: String?,
        @RequestParam(required = false) publisher: String?,
        @RequestParam(required = false) inLibrary: Boolean?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) order: String?,
    ): List<WishlistResponse> =
        wishlistService.search(
            jwt.userId(),
            WishlistFilters(
                name = name,
                genre = genre,
                developer = developer,
                publisher = publisher,
                inLibrary = inLibrary,
                sort = sort?.let { WishlistSort.parse(it) } ?: WishlistSort.ADDED,
                order = order?.let { WishlistOrder.parse(it) },
            ),
        ).map { WishlistResponse.from(it) }

    @DeleteMapping("/{gameId}")
    fun remove(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable gameId: Long,
    ): ResponseEntity<Void> {
        wishlistService.remove(jwt.userId(), gameId)
        return ResponseEntity.noContent().build()
    }
}