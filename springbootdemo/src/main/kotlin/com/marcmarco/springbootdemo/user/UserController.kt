package com.marcmarco.springbootdemo.user

import com.marcmarco.springbootdemo.user.dto.RegisterRequest
import com.marcmarco.springbootdemo.user.dto.LoginRequest
import com.marcmarco.springbootdemo.user.dto.UpdateProfileRequest
import com.marcmarco.springbootdemo.user.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @PostMapping
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        val user = userService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): UserResponse {
        val user = userService.login(request)
        return UserResponse.from(user)
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): UserResponse =
        UserResponse.from(userService.getUser(id))

    @PutMapping("/{id}")
    fun updateProfile(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): UserResponse = UserResponse.from(userService.updateProfile(id, request))

    @GetMapping
    fun listUsers(@RequestParam(required = false) username: String?): List<UserResponse> =
        userService.listUsers(username).map { UserResponse.from(it) }
}