package com.marcmarco.springbootdemo.nickname

import com.marcmarco.springbootdemo.nickname.dto.NicknameRequest
import com.marcmarco.springbootdemo.nickname.dto.NicknameResponse
import com.marcmarco.springbootdemo.security.userId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/nicknames")
class NicknameController(private val nicknameService: NicknameService) {

    @PutMapping("/{targetId}")
    fun setNickname(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable targetId: Long,
        @Valid @RequestBody request: NicknameRequest,
    ): NicknameResponse = NicknameResponse.from(nicknameService.setNickname(jwt.userId(), targetId, request))

    @GetMapping
    fun listNicknames(@AuthenticationPrincipal jwt: Jwt): List<NicknameResponse> =
        nicknameService.listNicknames(jwt.userId()).map { NicknameResponse.from(it) }

    @GetMapping("/{targetId}")
    fun getNickname(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable targetId: Long,
    ): NicknameResponse = NicknameResponse.from(nicknameService.getNickname(jwt.userId(), targetId))

    @DeleteMapping("/{targetId}")
    fun removeNickname(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable targetId: Long,
    ): ResponseEntity<Void> {
        nicknameService.removeNickname(jwt.userId(), targetId)
        return ResponseEntity.noContent().build()
    }
}