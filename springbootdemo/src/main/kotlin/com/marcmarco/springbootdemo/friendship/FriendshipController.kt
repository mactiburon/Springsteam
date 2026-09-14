package com.marcmarco.springbootdemo.friendship

import com.marcmarco.springbootdemo.friendship.dto.FriendshipRequest
import com.marcmarco.springbootdemo.friendship.dto.FriendshipResponse
import com.marcmarco.springbootdemo.security.userId
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
@RequestMapping("/api/friendships")
class FriendshipController(private val friendshipService: FriendshipService) {

    @PostMapping
    fun sendRequest(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: FriendshipRequest,
    ): ResponseEntity<FriendshipResponse> {
        val friendship = friendshipService.sendRequest(jwt.userId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(FriendshipResponse.from(friendship))
    }

    @GetMapping
    fun search(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) status: FriendshipStatus?,
    ): List<FriendshipResponse> =
        friendshipService.search(jwt.userId(), status).map { FriendshipResponse.from(it) }

    @GetMapping("/{id}")
    fun getFriendship(@PathVariable id: Long): FriendshipResponse =
        FriendshipResponse.from(friendshipService.getFriendship(id))

    @PutMapping("/{id}/accept")
    fun accept(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
    ): FriendshipResponse = FriendshipResponse.from(friendshipService.accept(id, jwt.userId()))

    @PutMapping("/{id}/block")
    fun block(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
    ): FriendshipResponse = FriendshipResponse.from(friendshipService.block(id, jwt.userId()))

    @DeleteMapping("/{id}")
    fun remove(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        friendshipService.remove(id, jwt.userId())
        return ResponseEntity.noContent().build()
    }
}