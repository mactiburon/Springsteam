package com.marcmarco.springbootdemo.nickname

import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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

    private fun registerUser(prefix: String): Long {
        val body = jsonBody(
            mapOf(
                "username" to "${prefix}_$suffix",
                "email" to "${prefix}_$suffix@test.com",
                "password" to "secreto123",
            ),
        )
        val response = mockMvc.perform(
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        return objectMapper.readTree(response.contentAsString).path("id").asLong()
    }

    private fun makeFriends(userId: Long, otherId: Long) {
        val request = jsonBody(mapOf("requesterId" to userId, "addresseeId" to otherId))
        val response = mockMvc.perform(
            post("/api/friendships").contentType(MediaType.APPLICATION_JSON).content(request),
        )
            .andExpect(status().isCreated)
            .andReturn().response
        val friendshipId = objectMapper.readTree(response.contentAsString).path("id").asLong()
        mockMvc.perform(put("/api/friendships/$friendshipId/accept").param("userId", otherId.toString()))
            .andExpect(status().isOk)
    }

    // ===== Poner mote =====

    @Test
    fun `poner mote a un amigo devuelve 200`() {
        val alice = registerUser("moteA")
        val bob = registerUser("moteB")
        makeFriends(alice, bob)
        mockMvc.perform(
            put("/api/nicknames/$bob")
                .param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "El Rápido"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("El Rápido"))
            .andExpect(jsonPath("$.ownerId").value(alice))
            .andExpect(jsonPath("$.targetId").value(bob))
            .andExpect(jsonPath("$.target.username").value("moteB_$suffix"))
    }

    @Test
    fun `renovar el mote sobrescribe el valor anterior`() {
        val alice = registerUser("renewA")
        val bob = registerUser("renewB")
        makeFriends(alice, bob)
        val putBody = jsonBody(mapOf("nickname" to "Primero"))
        mockMvc.perform(
            put("/api/nicknames/$bob").param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON).content(putBody),
        ).andExpect(status().isOk)
        mockMvc.perform(
            put("/api/nicknames/$bob").param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "Segundo"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("Segundo"))
    }

    @Test
    fun `no puedes ponerte un mote a ti mismo devuelve 400`() {
        val alice = registerUser("autof")
        mockMvc.perform(
            put("/api/nicknames/$alice")
                .param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "Solo"))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `poner mote a un no amigo devuelve 400`() {
        val alice = registerUser("noamiA")
        val carol = registerUser("noamiC")
        mockMvc.perform(
            put("/api/nicknames/$carol")
                .param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "Intruso"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Solo puedes poner un mote a un usuario con el que seas amigo"))
    }

    @Test
    fun `poner mote a usuario inexistente devuelve 404`() {
        val alice = registerUser("fantas")
        mockMvc.perform(
            put("/api/nicknames/999999999")
                .param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to "Fantasma"))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `mote vacio devuelve 400`() {
        val alice = registerUser("vacioA")
        val bob = registerUser("vacioB")
        makeFriends(alice, bob)
        mockMvc.perform(
            put("/api/nicknames/$bob")
                .param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("nickname" to " "))),
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Obtener y listar =====

    @Test
    fun `obtener mote por targetId devuelve 200`() {
        val alice = registerUser("getmoteA")
        val bob = registerUser("getmoteB")
        makeFriends(alice, bob)
        mockMvc.perform(
            put("/api/nicknames/$bob").param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "Cuñao"))),
        ).andExpect(status().isOk)
        mockMvc.perform(get("/api/nicknames/$bob").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("Cuñao"))
    }

    @Test
    fun `obtener mote inexistente devuelve 404`() {
        val alice = registerUser("nomoteA")
        val bob = registerUser("nomoteB")
        makeFriends(alice, bob)
        mockMvc.perform(get("/api/nicknames/$bob").param("userId", alice.toString()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `listar motes devuelve todos los del usuario`() {
        val alice = registerUser("listaA")
        val bob = registerUser("listaB")
        val carol = registerUser("listaC")
        makeFriends(alice, bob)
        makeFriends(alice, carol)
        mockMvc.perform(
            put("/api/nicknames/$bob").param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "Listo1"))),
        ).andExpect(status().isOk)
        mockMvc.perform(
            put("/api/nicknames/$carol").param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "Listo2"))),
        ).andExpect(status().isOk)
        mockMvc.perform(get("/api/nicknames").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].nickname", hasItem("Listo1")))
            .andExpect(jsonPath("$[*].nickname", hasItem("Listo2")))
    }

    @Test
    fun `los motes son privados de cada usuario`() {
        val alice = registerUser("privA")
        val bob = registerUser("privB")
        val carol = registerUser("privC")
        makeFriends(alice, bob)
        makeFriends(carol, bob)
        mockMvc.perform(
            put("/api/nicknames/$bob").param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "SecretoA"))),
        ).andExpect(status().isOk)
        mockMvc.perform(
            put("/api/nicknames/$bob").param("userId", carol.toString())
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "SecretoC"))),
        ).andExpect(status().isOk)
        mockMvc.perform(get("/api/nicknames").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].nickname", hasItem("SecretoA")))
            .andExpect(jsonPath("$[*].nickname", not(hasItem("SecretoC"))))
    }

    @Test
    fun `listar sin motes devuelve lista vacia`() {
        val alice = registerUser("vacia")
        mockMvc.perform(get("/api/nicknames").param("userId", alice.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ===== Borrar =====

    @Test
    fun `borrar mote devuelve 204 y deja de existir`() {
        val alice = registerUser("delA")
        val bob = registerUser("delB")
        makeFriends(alice, bob)
        mockMvc.perform(
            put("/api/nicknames/$bob").param("userId", alice.toString())
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("nickname" to "Efimero"))),
        ).andExpect(status().isOk)
        mockMvc.perform(delete("/api/nicknames/$bob").param("userId", alice.toString()))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/nicknames/$bob").param("userId", alice.toString()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `borrar mote inexistente devuelve 404`() {
        val alice = registerUser("delnoA")
        val bob = registerUser("delnoB")
        makeFriends(alice, bob)
        mockMvc.perform(delete("/api/nicknames/$bob").param("userId", alice.toString()))
            .andExpect(status().isNotFound)
    }
}