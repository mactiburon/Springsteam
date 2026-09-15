package com.marcmarco.library

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WishlistControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jwtEncoder: JwtEncoder

    @Autowired
    lateinit var gameRepository: GameRepository

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private var userIdCounter = 3_000_000L

    private var gameIdCounter = 900_000L

    private fun jsonBody(pairs: Map<String, Any?>): String =
        objectMapper.writeValueAsString(pairs.filterValues { it != null })

    private data class Session(val userId: Long, val token: String)

    private fun session(): Session {
        val userId = userIdCounter++
        return Session(userId = userId, token = jwtEncoder.testToken(userId))
    }

    private fun createGame(
        id: Long,
        name: String,
        genre: String? = null,
        developer: String? = null,
        publisher: String? = null,
        releaseDate: LocalDate? = null,
    ): Long {
        gameRepository.save(
            Game(
                id = id,
                name = name,
                genre = genre,
                developer = developer,
                publisher = publisher,
                releaseDate = releaseDate,
            ),
        )
        return id
    }

    private fun addToWishlist(token: String, gameId: Long) =
        mockMvc.perform(
            post("/api/wishlist")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to gameId))),
        ).andExpect(status().isCreated)

    private fun addToLibrary(token: String, gameId: Long) =
        mockMvc.perform(
            post("/api/library")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to gameId))),
        ).andExpect(status().isCreated)

    // ===== Añadir =====

    @Test
    fun `anadir juego a wishlist devuelve 201 con el juego anidado`() {
        val session = session()
        val gameId = createGame(gameIdCounter++, "CelesteEnsueño_$suffix", genre = "Platformer")
        mockMvc.perform(
            post("/api/wishlist")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to gameId))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(session.userId))
            .andExpect(jsonPath("$.gameId").value(gameId))
            .andExpect(jsonPath("$.game.name").value("CelesteEnsueño_$suffix"))
            .andExpect(jsonPath("$.game.genre").value("Platformer"))
            .andExpect(jsonPath("$.addedAt").exists())
    }

    @Test
    fun `anadir juego sin token devuelve 401`() {
        mockMvc.perform(
            post("/api/wishlist").contentType(MediaType.APPLICATION_JSON).content(jsonBody(mapOf("gameId" to 1L))),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `anadir wishlist sin gameId devuelve 400`() {
        val session = session()
        mockMvc.perform(
            post("/api/wishlist")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `anadir juego inexistente en el catalogo devuelve 404`() {
        val session = session()
        mockMvc.perform(
            post("/api/wishlist")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to 999_999_999L))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `anadir juego duplicado devuelve 409`() {
        val session = session()
        val gameId = createGame(gameIdCounter++, "Repetido_$suffix")
        addToWishlist(session.token, gameId)
        mockMvc.perform(
            post("/api/wishlist")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody(mapOf("gameId" to gameId))),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `el mismo juego en la wishlist de otro usuario es permitido`() {
        val sessionA = session()
        val sessionB = session()
        val gameId = createGame(gameIdCounter++, "Compartido_$suffix")
        addToWishlist(sessionA.token, gameId)
        addToWishlist(sessionB.token, gameId)
    }

    // ===== Listar / buscar =====

    @Test
    fun `listar wishlist vacia devuelve lista vacia`() {
        val session = session()
        mockMvc.perform(get("/api/wishlist").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `listar devuelve solo los del usuario en orden de fecha anadida descendente`() {
        val session = session()
        val first = createGame(gameIdCounter++, "Primero_$suffix")
        val second = createGame(gameIdCounter++, "Segundo_$suffix")
        addToWishlist(session.token, first)
        Thread.sleep(10)
        addToWishlist(session.token, second)

        mockMvc.perform(get("/api/wishlist").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(hasSize<Any>(2)))
            .andExpect(jsonPath("$[0].gameId").value(second))
            .andExpect(jsonPath("$[1].gameId").value(first))
    }

    @Test
    fun `listar no muestra la wishlist de otro usuario`() {
        val sessionA = session()
        val sessionB = session()
        val gameId = createGame(gameIdCounter++, "Privada_$suffix")
        addToWishlist(sessionA.token, gameId)
        mockMvc.perform(get("/api/wishlist").header(HttpHeaders.AUTHORIZATION, "Bearer ${sessionB.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `buscar por nombre parcial e ignorando mayusculas`() {
        val session = session()
        val idKeep = createGame(gameIdCounter++, "Hollow_Cs_$suffix")
        val idSkip = createGame(gameIdCounter++, "Celeste_Cs_$suffix")
        addToWishlist(session.token, idKeep)
        addToWishlist(session.token, idSkip)

        mockMvc.perform(
            get("/api/wishlist").param("name", "HOLLOW")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].gameId").value(idKeep))
    }

    @Test
    fun `filtrar por genero`() {
        val session = session()
        val idAction = createGame(gameIdCounter++, "AccionG_$suffix", genre = "Action")
        val idRpg = createGame(gameIdCounter++, "RolG_$suffix", genre = "RPG")
        addToWishlist(session.token, idAction)
        addToWishlist(session.token, idRpg)

        mockMvc.perform(
            get("/api/wishlist").param("genre", "rpg")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].gameId").value(idRpg))
    }

    @Test
    fun `filtrar por desarrollador`() {
        val session = session()
        val idTeamCherry = createGame(gameIdCounter++, "Cherry_$suffix", developer = "Team Cherry")
        val idOtroDev = createGame(gameIdCounter++, "OtroDev_$suffix", developer = "Estudio X")
        addToWishlist(session.token, idTeamCherry)
        addToWishlist(session.token, idOtroDev)

        mockMvc.perform(
            get("/api/wishlist").param("developer", "team cherry")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].gameId").value(idTeamCherry))
    }

    @Test
    fun `filtrar por editor (publisher)`() {
        val session = session()
        val idEditA = createGame(gameIdCounter++, "EditA_$suffix", publisher = "Devolver")
        val idEditB = createGame(gameIdCounter++, "EditB_$suffix", publisher = "EA")
        addToWishlist(session.token, idEditA)
        addToWishlist(session.token, idEditB)

        mockMvc.perform(
            get("/api/wishlist").param("publisher", "devolver")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].gameId").value(idEditA))
    }

    @Test
    fun `inLibrary true devuelve solo juegos ya en biblioteca`() {
        val session = session()
        val owned = createGame(gameIdCounter++, "Comprado_$suffix")
        val wanted = createGame(gameIdCounter++, "Deseado_$suffix")
        addToLibrary(session.token, owned)
        addToWishlist(session.token, owned)
        addToWishlist(session.token, wanted)

        mockMvc.perform(
            get("/api/wishlist").param("inLibrary", "true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].gameId").value(owned))
    }

    @Test
    fun `inLibrary false devuelve solo juegos no en biblioteca`() {
        val session = session()
        val owned = createGame(gameIdCounter++, "Tengo_$suffix")
        val wanted = createGame(gameIdCounter++, "Quiero_$suffix")
        addToLibrary(session.token, owned)
        addToWishlist(session.token, owned)
        addToWishlist(session.token, wanted)

        mockMvc.perform(
            get("/api/wishlist").param("inLibrary", "false")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].gameId").value(wanted))
    }

    // ===== Orden =====

    @Test
    fun `ordenar por nombre ascendente`() {
        val session = session()
        val idB = createGame(gameIdCounter++, "Bravo_$suffix")
        val idA = createGame(gameIdCounter++, "Alfa_$suffix")
        val idC = createGame(gameIdCounter++, "Charlie_$suffix")
        addToWishlist(session.token, idB)
        addToWishlist(session.token, idA)
        addToWishlist(session.token, idC)

        mockMvc.perform(
            get("/api/wishlist").param("sort", "name").param("order", "asc")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].gameId").value(idA))
            .andExpect(jsonPath("$[1].gameId").value(idB))
            .andExpect(jsonPath("$[2].gameId").value(idC))
    }

    @Test
    fun `ordenar por nombre descendente`() {
        val session = session()
        val idB = createGame(gameIdCounter++, "Bravo_$suffix")
        val idA = createGame(gameIdCounter++, "Alfa_$suffix")
        val idC = createGame(gameIdCounter++, "Charlie_$suffix")
        addToWishlist(session.token, idB)
        addToWishlist(session.token, idA)
        addToWishlist(session.token, idC)

        mockMvc.perform(
            get("/api/wishlist").param("sort", "name").param("order", "desc")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].gameId").value(idC))
            .andExpect(jsonPath("$[1].gameId").value(idB))
            .andExpect(jsonPath("$[2].gameId").value(idA))
    }

    @Test
    fun `ordenar por fecha anadida ascendente`() {
        val session = session()
        val first = createGame(gameIdCounter++, "PrimeroA_$suffix")
        val second = createGame(gameIdCounter++, "SegundoA_$suffix")
        addToWishlist(session.token, first)
        Thread.sleep(10)
        addToWishlist(session.token, second)

        mockMvc.perform(
            get("/api/wishlist").param("sort", "added").param("order", "asc")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].gameId").value(first))
            .andExpect(jsonPath("$[1].gameId").value(second))
    }

    @Test
    fun `sort invalido devuelve 400`() {
        val session = session()
        mockMvc.perform(
            get("/api/wishlist").param("sort", "precio")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"),
        )
            .andExpect(status().isBadRequest)
    }

    // ===== Borrar =====

    @Test
    fun `borrar juego de wishlist devuelve 204 y deja de listarse`() {
        val session = session()
        val gameId = createGame(gameIdCounter++, "Eliminable_$suffix")
        addToWishlist(session.token, gameId)
        mockMvc.perform(delete("/api/wishlist/$gameId").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/wishlist").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `borrar juego no en wishlist devuelve 404`() {
        val session = session()
        val gameId = createGame(gameIdCounter++, "NoEsta_$suffix")
        mockMvc.perform(delete("/api/wishlist/$gameId").header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `borrar juego de la wishlist de otro usuario devuelve 404`() {
        val sessionA = session()
        val sessionB = session()
        val gameId = createGame(gameIdCounter++, "Ajena_$suffix")
        addToWishlist(sessionA.token, gameId)
        mockMvc.perform(delete("/api/wishlist/$gameId").header(HttpHeaders.AUTHORIZATION, "Bearer ${sessionB.token}"))
            .andExpect(status().isNotFound)
    }
}