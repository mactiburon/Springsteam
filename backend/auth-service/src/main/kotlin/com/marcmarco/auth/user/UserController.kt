package com.marcmarco.auth.user

import com.marcmarco.auth.dto.ChangePasswordRequest
import com.marcmarco.auth.dto.DeleteAccountRequest
import com.marcmarco.auth.dto.LoginRequest
import com.marcmarco.auth.dto.LoginResponse
import com.marcmarco.auth.dto.RegisterRequest
import com.marcmarco.auth.dto.UpdateEmailRequest
import com.marcmarco.auth.dto.UpdateProfileRequest
import com.marcmarco.auth.dto.UpdateUsernameRequest
import com.marcmarco.auth.dto.UserResponse
import com.marcmarco.auth.security.AuthService
import com.marcmarco.auth.security.userId
import com.marcmarco.shared.error.NotFoundException
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
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val authService: AuthService,
) {

    @PostMapping
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        val user = userService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse = authService.login(request)

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): UserResponse =
        UserResponse.from(userService.getActiveUser(id))

    @PutMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): UserResponse = UserResponse.from(userService.updateProfile(jwt.userId(), request))

    @PutMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): UserResponse = UserResponse.from(userService.changePassword(jwt.userId(), request))

    @PutMapping("/me/email")
    fun updateEmail(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdateEmailRequest,
    ): UserResponse = UserResponse.from(userService.updateEmail(jwt.userId(), request))

    @PutMapping("/me/username")
    fun updateUsername(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdateUsernameRequest,
    ): UserResponse = UserResponse.from(userService.updateUsername(jwt.userId(), request))

    @DeleteMapping("/me/account")
    fun deactivateAccount(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: DeleteAccountRequest,
    ): ResponseEntity<Void> {
        userService.deactivateAccount(jwt.userId(), request)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun listUsers(@RequestParam(required = false) username: String?): List<UserResponse> =
        userService.listUsers(username).map { UserResponse.from(it) }
}