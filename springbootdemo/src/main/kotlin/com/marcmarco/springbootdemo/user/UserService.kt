package com.marcmarco.springbootdemo.user

import com.marcmarco.springbootdemo.common.exception.BadRequestException
import com.marcmarco.springbootdemo.common.exception.ConflictException
import com.marcmarco.springbootdemo.common.exception.NotFoundException
import com.marcmarco.springbootdemo.user.dto.ChangePasswordRequest
import com.marcmarco.springbootdemo.user.dto.DeleteAccountRequest
import com.marcmarco.springbootdemo.user.dto.RegisterRequest
import com.marcmarco.springbootdemo.user.dto.UpdateEmailRequest
import com.marcmarco.springbootdemo.user.dto.UpdateProfileRequest
import com.marcmarco.springbootdemo.user.dto.UpdateUsernameRequest
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

    fun getUser(id: Long): User =
        userRepository.findById(id)
            .orElseThrow { NotFoundException("Usuario con id $id no encontrado") }

    fun getActiveUser(id: Long): User {
        val user = getUser(id)
        if (!user.active) {
            throw NotFoundException("Usuario con id $id no encontrado")
        }
        return user
    }

    fun updateProfile(id: Long, request: UpdateProfileRequest): User {
        val user = getActiveUser(id)
        request.displayName?.let { user.displayName = it }
        request.bio?.let { user.bio = it }
        request.avatar?.let { user.avatar = it }
        return userRepository.save(user)
    }

    fun changePassword(id: Long, request: ChangePasswordRequest): User {
        val user = getActiveUser(id)
        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw BadRequestException("La contraseña actual no es correcta")
        }
        if (request.newPassword == request.currentPassword) {
            throw BadRequestException("La nueva contraseña debe ser diferente de la actual")
        }
        user.password = passwordEncoder.encode(request.newPassword)!!
        return userRepository.save(user)
    }

    fun updateEmail(id: Long, request: UpdateEmailRequest): User {
        val user = getActiveUser(id)
        if (user.email != request.email && userRepository.existsByEmail(request.email)) {
            throw ConflictException("El email '${request.email}' ya está registrado")
        }
        user.email = request.email
        return userRepository.save(user)
    }

    fun updateUsername(id: Long, request: UpdateUsernameRequest): User {
        val user = getActiveUser(id)
        if (user.username != request.username && userRepository.existsByUsername(request.username)) {
            throw ConflictException("El username '${request.username}' ya está en uso")
        }
        user.username = request.username
        return userRepository.save(user)
    }

    fun deactivateAccount(id: Long, request: DeleteAccountRequest) {
        val user = getActiveUser(id)
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw BadRequestException("La contraseña no es correcta para eliminar la cuenta")
        }
        user.active = false
        userRepository.save(user)
    }

    fun listUsers(username: String? = null): List<User> =
        if (username.isNullOrBlank()) {
            userRepository.findAllByActiveTrue()
        } else {
            userRepository.findByUsernameContainingIgnoreCaseAndActiveTrue(username)
        }
}