package com.marcmarco.springbootdemo.user

import com.marcmarco.springbootdemo.common.exception.ConflictException
import com.marcmarco.springbootdemo.common.exception.InvalidCredentialsException
import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.user.dto.RegisterRequest
import com.marcmarco.springbootdemo.user.dto.LoginRequest
import com.marcmarco.springbootdemo.user.dto.UpdateProfileRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun register(request: RegisterRequest): User {
        if (userRepository.existsByUsername(request.username)) {
            throw ConflictException("El username '${request.username}' ya está en uso")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("El email '${request.email}' ya está registrado")
        }

        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password)!!,
            displayName = request.displayName ?: request.username,
        )
        return userRepository.save(user)
    }

    fun login(request: LoginRequest): User {
        val user = userRepository.findByUsername(request.identifier)
            ?: userRepository.findByEmail(request.identifier)
            ?: throw InvalidCredentialsException("Credenciales incorrectas")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException("Credenciales incorrectas")
        }
        return user
    }

    fun getUser(id: Long): User =
        userRepository.findById(id)
            .orElseThrow { NotFoundException("Usuario con id $id no encontrado") }

    fun updateProfile(id: Long, request: UpdateProfileRequest): User {
        val user = getUser(id)
        request.displayName?.let { user.displayName = it }
        request.bio?.let { user.bio = it }
        request.avatar?.let { user.avatar = it }
        return userRepository.save(user)
    }

    fun listUsers(username: String? = null): List<User> =
        if (username.isNullOrBlank()) {
            userRepository.findAll()
        } else {
            userRepository.findByUsernameContainingIgnoreCase(username)
        }
}