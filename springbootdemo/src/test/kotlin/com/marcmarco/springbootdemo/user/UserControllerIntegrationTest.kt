package com.marcmarco.springbootdemo.user

import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private fun uniqueUser() = "test_$suffix"

    private fun uniqueEmail() = "$suffix@test.com"

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private fun register(username: String, email: String, password: String = "secreto123"): Long {
        val body = jsonBody(
            mapOf(
                "username" to username,
                "email" to email,
                "password" to password,
            ),
        )
        val response = mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun loginToken(username: String, password: String = "secreto123"): String {
        val body = jsonBody(mapOf("identifier" to username, "password" to password))
        val response = mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("token").asText()
    }

    // ===== Registro =====

    @Test
    fun `registro exitoso devuelve 201 y nunca expone la password`() {
        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    jsonBody(
                        mapOf(
                            "username" to uniqueUser(),
                            "email" to uniqueEmail(),
                            "password" to "secreto123",
                            "displayName" to "Nombre Visible",
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.username").value(uniqueUser()))
            .andExpect(jsonPath("$.displayName").value("Nombre Visible"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))))
    }

    @Test
    fun `si no se envia displayName usa el username como valor por defecto`() {
        val username = uniqueUser()
        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("username" to username, "email" to uniqueEmail(), "password" to "secreto123"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.displayName").value(username))
    }

    @Test
    fun `username duplicado devuelve 409`() {
        val username = uniqueUser()
        val email = uniqueEmail()
        register(username, email)
        val body = jsonBody(mapOf("username" to username, "email" to "otro@test.com", "password" to "secreto123"))
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("El username '$username' ya está en uso"))
    }

    @Test
    fun `email duplicado devuelve 409`() {
        val username = uniqueUser()
        val email = uniqueEmail()
        register(username, email)
        val body = jsonBody(mapOf("username" to "otro_$suffix", "email" to email, "password" to "secreto123"))
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("El email '$email' ya está registrado"))
    }

    @Test
    fun `email invalido devuelve 400`() {
        val body = jsonBody(mapOf("username" to uniqueUser(), "email" to "correo-malo", "password" to "secreto123"))
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("email: El email no tiene un formato válido"))
    }

    @Test
    fun `password demasiado corta devuelve 400`() {
        val body = jsonBody(mapOf("username" to uniqueUser(), "email" to uniqueEmail(), "password" to "123"))
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    // ===== Login =====

    @Test
    fun `login con username correcto devuelve 200 con token JWT`() {
        val username = uniqueUser()
        register(username, uniqueEmail(), "secreto123")
        val body = jsonBody(mapOf("identifier" to username, "password" to "secreto123"))
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber)
            .andExpect(jsonPath("$.user.username").value(username))
    }

    @Test
    fun `login con email correcto devuelve 200`() {
        val email = uniqueEmail()
        register(uniqueUser(), email, "secreto123")
        val body = jsonBody(mapOf("identifier" to email, "password" to "secreto123"))
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.user.email").value(email))
    }

    @Test
    fun `login con password incorrecta devuelve 401 sin revelar detalles`() {
        val username = uniqueUser()
        register(username, uniqueEmail(), "secreto123")
        val body = jsonBody(mapOf("identifier" to username, "password" to "incorrecta"))
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Credenciales incorrectas"))
    }

    @Test
    fun `login con usuario inexistente devuelve 401`() {
        val body = jsonBody(mapOf("identifier" to "no_existe_$suffix", "password" to "secreto123"))
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }

    // ===== Obtener perfil =====

    @Test
    fun `obtener usuario por id devuelve 200`() {
        val username = uniqueUser()
        val id = register(username, uniqueEmail())
        mockMvc.perform(get("/api/users/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.username").value(username))
    }

    @Test
    fun `usuario inexistente devuelve 404`() {
        mockMvc.perform(get("/api/users/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Usuario con id 999999999 no encontrado"))
    }

    // ===== Editar perfil =====

    @Test
    fun `editar perfil solo modifica los campos enviados`() {
        val username = uniqueUser()
        register(username, uniqueEmail())
        val token = loginToken(username)
        val body = jsonBody(mapOf("bio" to "Jugador desde siempre", "avatar" to "https://miweb.com/avatar.png"))
        mockMvc.perform(
            put("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value(username))
            .andExpect(jsonPath("$.bio").value("Jugador desde siempre"))
            .andExpect(jsonPath("$.avatar").value("https://miweb.com/avatar.png"))
    }

    @Test
    fun `editar perfil sin token devuelve 401`() {
        val body = jsonBody(mapOf("bio" to "hola"))
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }

    // ===== Listar y buscar =====

    @Test
    fun `listar usuarios devuelve todos`() {
        val username = uniqueUser()
        register(username, uniqueEmail())
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].username", hasItem(username)))
    }

    @Test
    fun `buscar por username devuelve solo los que coinciden`() {
        val matchUser = "alpha_$suffix"
        val otherUser = "beta_$suffix"
        register(matchUser, "a_$suffix@test.com")
        register(otherUser, "b_$suffix@test.com")
        mockMvc.perform(get("/api/users").param("username", "alpha_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].username", hasItem(matchUser)))
            .andExpect(jsonPath("$[*].username", org.hamcrest.Matchers.not(hasItem(otherUser))))
    }

    @Test
    fun `buscar sin coincidencias devuelve lista vacia`() {
        mockMvc.perform(get("/api/users").param("username", "no_existe_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ===== Cambiar contraseña =====

    @Test
    fun `cambiar password permite login con la nueva y rechaza la antigua`() {
        val username = uniqueUser()
        register(username, uniqueEmail(), "secreto123")
        val token = loginToken(username)
        val body = jsonBody(mapOf("currentPassword" to "secreto123", "newPassword" to "nuevaClave6"))
        mockMvc.perform(
            put("/api/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)

        val oldLogin = jsonBody(mapOf("identifier" to username, "password" to "secreto123"))
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(oldLogin))
            .andExpect(status().isUnauthorized)

        val newLogin = jsonBody(mapOf("identifier" to username, "password" to "nuevaClave6"))
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(newLogin))
            .andExpect(status().isOk)
    }

    @Test
    fun `cambiar password con la actual incorrecta devuelve 400`() {
        val username = uniqueUser()
        register(username, uniqueEmail(), "secreto123")
        val token = loginToken(username)
        val body = jsonBody(mapOf("currentPassword" to "incorrecta", "newPassword" to "nuevaClave6"))
        mockMvc.perform(
            put("/api/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("La contraseña actual no es correcta"))
    }

    @Test
    fun `cambiar password con la nueva igual a la actual devuelve 400`() {
        val username = uniqueUser()
        register(username, uniqueEmail(), "secreto123")
        val token = loginToken(username)
        val body = jsonBody(mapOf("currentPassword" to "secreto123", "newPassword" to "secreto123"))
        mockMvc.perform(
            put("/api/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("La nueva contraseña debe ser diferente de la actual"))
    }

    @Test
    fun `cambiar password con la nueva demasiado corta devuelve 400`() {
        val username = uniqueUser()
        register(username, uniqueEmail(), "secreto123")
        val token = loginToken(username)
        val body = jsonBody(mapOf("currentPassword" to "secreto123", "newPassword" to "123"))
        mockMvc.perform(
            put("/api/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `cambiar password sin token devuelve 401`() {
        val body = jsonBody(mapOf("currentPassword" to "secreto123", "newPassword" to "nuevaClave6"))
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }

    // ===== Actualizar email =====

    @Test
    fun `actualizar email devuelve 200 con el nuevo email`() {
        val username = uniqueUser()
        register(username, uniqueEmail())
        val token = loginToken(username)
        val newEmail = "nuevo_$suffix@test.com"
        val body = jsonBody(mapOf("email" to newEmail))
        mockMvc.perform(
            put("/api/users/me/email")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(newEmail))
    }

    @Test
    fun `actualizar email a uno ya usado por otro usuario devuelve 409`() {
        val takenEmail = uniqueEmail()
        register("owner_$suffix", takenEmail)
        val username = uniqueUser()
        register(username, "otro_$suffix@test.com")
        val token = loginToken(username)
        val body = jsonBody(mapOf("email" to takenEmail))
        mockMvc.perform(
            put("/api/users/me/email")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("El email '$takenEmail' ya está registrado"))
    }

    @Test
    fun `actualizar email al propio deja la cuenta intacta`() {
        val username = uniqueUser()
        val email = uniqueEmail()
        register(username, email)
        val token = loginToken(username)
        val body = jsonBody(mapOf("email" to email))
        mockMvc.perform(
            put("/api/users/me/email")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(email))
    }

    @Test
    fun `actualizar email con formato invalido devuelve 400`() {
        val username = uniqueUser()
        register(username, uniqueEmail())
        val token = loginToken(username)
        val body = jsonBody(mapOf("email" to "correo-malo"))
        mockMvc.perform(
            put("/api/users/me/email")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Actualizar username =====

    @Test
    fun `actualizar username devuelve 200 con el nuevo username`() {
        val username = uniqueUser()
        register(username, uniqueEmail())
        val token = loginToken(username)
        val newUsername = "nuevo_$suffix"
        val body = jsonBody(mapOf("username" to newUsername))
        mockMvc.perform(
            put("/api/users/me/username")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value(newUsername))
    }

    @Test
    fun `actualizar username a uno ya usado por otro usuario devuelve 409`() {
        val takenUsername = "owner_$suffix"
        register(takenUsername, "owner_$suffix@test.com")
        val username = uniqueUser()
        register(username, uniqueEmail())
        val token = loginToken(username)
        val body = jsonBody(mapOf("username" to takenUsername))
        mockMvc.perform(
            put("/api/users/me/username")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("El username '$takenUsername' ya está en uso"))
    }

    @Test
    fun `actualizar username al propio deja la cuenta intacta`() {
        val username = uniqueUser()
        register(username, uniqueEmail())
        val token = loginToken(username)
        val body = jsonBody(mapOf("username" to username))
        mockMvc.perform(
            put("/api/users/me/username")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value(username))
    }

    // ===== Desactivar cuenta =====

    @Test
    fun `desactivar cuenta con password correcta devuelve 204 y bloquea login y perfil`() {
        val username = uniqueUser()
        val id = register(username, uniqueEmail())
        val token = loginToken(username)
        val body = jsonBody(mapOf("password" to "secreto123"))
        mockMvc.perform(
            delete("/api/users/me/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isNoContent)

        val loginBody = jsonBody(mapOf("identifier" to username, "password" to "secreto123"))
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/users/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `desactivar cuenta con password incorrecta devuelve 400`() {
        val username = uniqueUser()
        register(username, uniqueEmail())
        val token = loginToken(username)
        val body = jsonBody(mapOf("password" to "incorrecta"))
        mockMvc.perform(
            delete("/api/users/me/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("La contraseña no es correcta para eliminar la cuenta"))
    }

    @Test
    fun `desactivar cuenta sin token devuelve 401`() {
        val body = jsonBody(mapOf("password" to "secreto123"))
        mockMvc.perform(delete("/api/users/me/account").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }
}