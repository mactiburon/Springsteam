package com.marcmarco.springbootdemo.nickname

import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NicknameControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private data class Session(val userId: Long, val token: String)

    private fun registerLogin(prefix: String): Session {
        val username = "${prefix}_$suffix"
        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("username" to username, "email" to "${prefix}_$suffix@test.com", "password" to "secreto123"))),
        )
            .andExpect(status().isCreated)
        val response = mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("identifier" to username, "password" to "secreto123"))),
        )
            .andExpect(status().isOk)
            .andReturn().response
        val tree = objectMapper.readTree(response.contentAsString)
        return Session(userId = tree.path("user").path("id").asLong(), token = tree.path("token").asText())
    }

    private fun makeFriends(owner: Session, target: Session) {
        val request = jsonBody(mapOf("addresseeId" to target.userId))
        val response = mockMvc.perform(
            post("/api/friendships")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        val friendshipId = objectMapper.readTree(response.contentAsString).path("id").asLong()
        mockMvc.perform(
            put("/api/friendships/$friendshipId/accept")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${target.token}"),
        )
            .andExpect(status().isOk)
    }

    // ===== Poner mote =====

    @Test
    fun `poner mote a un amigo devuelve 200`() {
        val alice = registerLogin("moteA")
        val bob = registerLogin("moteB")
        makeFriends(alice, bob)
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "El Rápido"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("El Rápido"))
            .andExpect(jsonPath("$.ownerId").value(alice.userId))
            .andExpect(jsonPath("$.targetId").value(bob.userId))
            .andExpect(jsonPath("$.target.username").value("moteB_$suffix"))
    }

    @Test
    fun `renovar el mote sobrescribe el valor anterior`() {
        val alice = registerLogin("renewA")
        val bob = registerLogin("renewB")
        makeFriends(alice, bob)
        val putBody = jsonBody(mapOf("nickname" to "Primero"))
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON).content(putBody),
        ).andExpect(status().isOk)
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "Segundo"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("Segundo"))
    }

    @Test
    fun `no puedes ponerte un mote a ti mismo devuelve 400`() {
        val alice = registerLogin("autof")
        mockMvc.perform(
            put("/api/nicknames/${alice.userId}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "Solo"))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `poner mote a un no amigo devuelve 400`() {
        val alice = registerLogin("noamiA")
        val carol = registerLogin("noamiC")
        mockMvc.perform(
            put("/api/nicknames/${carol.userId}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "Intruso"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Solo puedes poner un mote a un usuario con el que seas amigo"))
    }

    @Test
    fun `poner mote a usuario inexistente devuelve 404`() {
        val alice = registerLogin("fantas")
        mockMvc.perform(
            put("/api/nicknames/999999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "Fantasma"))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `mote vacio devuelve 400`() {
        val alice = registerLogin("vacioA")
        val bob = registerLogin("vacioB")
        makeFriends(alice, bob)
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to " "))),
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Obtener y listar =====

    @Test
    fun `obtener mote por targetId devuelve 200`() {
        val alice = registerLogin("getmoteA")
        val bob = registerLogin("getmoteB")
        makeFriends(alice, bob)
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "Cuñao"))),
        ).andExpect(status().isOk)
        mockMvc.perform(get("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("Cuñao"))
    }

    @Test
    fun `obtener mote inexistente devuelve 404`() {
        val alice = registerLogin("nomoteA")
        val bob = registerLogin("nomoteB")
        makeFriends(alice, bob)
        mockMvc.perform(get("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `listar motes devuelve todos los del usuario`() {
        val alice = registerLogin("listaA")
        val bob = registerLogin("listaB")
        val carol = registerLogin("listaC")
        makeFriends(alice, bob)
        makeFriends(alice, carol)
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "Listo1"))),
        ).andExpect(status().isOk)
        mockMvc.perform(
            put("/api/nicknames/${carol.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "Listo2"))),
        ).andExpect(status().isOk)
        mockMvc.perform(get("/api/nicknames").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].nickname", hasItem("Listo1")))
            .andExpect(jsonPath("$[*].nickname", hasItem("Listo2")))
    }

    @Test
    fun `los motes son privados de cada usuario`() {
        val alice = registerLogin("privA")
        val bob = registerLogin("privB")
        val carol = registerLogin("privC")
        makeFriends(alice, bob)
        makeFriends(carol, bob)
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "SecretoA"))),
        ).andExpect(status().isOk)
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${carol.token}")
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "SecretoC"))),
        ).andExpect(status().isOk)
        mockMvc.perform(get("/api/nicknames").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].nickname", hasItem("SecretoA")))
            .andExpect(jsonPath("$[*].nickname", not(hasItem("SecretoC"))))
    }

    @Test
    fun `listar sin motes devuelve lista vacia`() {
        val alice = registerLogin("vacia")
        mockMvc.perform(get("/api/nicknames").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ===== Borrar =====

    @Test
    fun `borrar mote devuelve 204 y deja de existir`() {
        val alice = registerLogin("delA")
        val bob = registerLogin("delB")
        makeFriends(alice, bob)
        mockMvc.perform(
            put("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}")
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "Efimero"))),
        ).andExpect(status().isOk)
        mockMvc.perform(delete("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `borrar mote inexistente devuelve 404`() {
        val alice = registerLogin("delnoA")
        val bob = registerLogin("delnoB")
        makeFriends(alice, bob)
        mockMvc.perform(delete("/api/nicknames/${bob.userId}").header(HttpHeaders.AUTHORIZATION, "Bearer ${alice.token}"))
            .andExpect(status().isNotFound)
    }
}