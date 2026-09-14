package com.marcmarco.springbootdemo.friendship

import com.marcmarco.springbootdemo.friendship.dto.FriendshipRequest
import com.marcmarco.springbootdemo.friendship.dto.FriendshipResponse
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
@RequestMapping("/api/friendships")
class FriendshipController(private val friendshipService: FriendshipService) {

    @PostMapping
    fun sendRequest(@Valid @RequestBody request: FriendshipRequest): ResponseEntity<FriendshipResponse> {
        val friendship = friendshipService.sendRequest(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(FriendshipResponse.from(friendship))
    }

    @GetMapping
    fun search(
        @RequestParam userId: Long,
        @RequestParam(required = false) status: FriendshipStatus?,
    ): List<FriendshipResponse> =
        friendshipService.search(userId, status).map { FriendshipResponse.from(it) }

    @GetMapping("/{id}")
    fun getFriendship(@PathVariable id: Long): FriendshipResponse =
        FriendshipResponse.from(friendshipService.getFriendship(id))

    @PutMapping("/{id}/accept")
    fun accept(
        @PathVariable id: Long,
        @RequestParam userId: Long,
    ): FriendshipResponse = FriendshipResponse.from(friendshipService.accept(id, userId))

    @PutMapping("/{id}/block")
    fun block(
        @PathVariable id: Long,
        @RequestParam userId: Long,
    ): FriendshipResponse = FriendshipResponse.from(friendshipService.block(id, userId))

    @DeleteMapping("/{id}")
    fun remove(
        @PathVariable id: Long,
        @RequestParam userId: Long,
    ): ResponseEntity<Void> {
        friendshipService.remove(id, userId)
        return ResponseEntity.noContent().build()
    }
}