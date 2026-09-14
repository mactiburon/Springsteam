package com.marcmarco.springbootdemo.nickname

import com.marcmarco.springbootdemo.nickname.dto.NicknameRequest
import com.marcmarco.springbootdemo.nickname.dto.NicknameResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/nicknames")
class NicknameController(private val nicknameService: NicknameService) {

    @PutMapping("/{targetId}")
    fun setNickname(
        @PathVariable targetId: Long,
        @RequestParam userId: Long,
        @Valid @RequestBody request: NicknameRequest,
    ): NicknameResponse = NicknameResponse.from(nicknameService.setNickname(userId, targetId, request))

    @GetMapping
    fun listNicknames(@RequestParam userId: Long): List<NicknameResponse> =
        nicknameService.listNicknames(userId).map { NicknameResponse.from(it) }

    @GetMapping("/{targetId}")
    fun getNickname(
        @PathVariable targetId: Long,
        @RequestParam userId: Long,
    ): NicknameResponse = NicknameResponse.from(nicknameService.getNickname(userId, targetId))

    @DeleteMapping("/{targetId}")
    fun removeNickname(
        @PathVariable targetId: Long,
        @RequestParam userId: Long,
    ): ResponseEntity<Void> {
        nicknameService.removeNickname(userId, targetId)
        return ResponseEntity.noContent().build()
    }
}