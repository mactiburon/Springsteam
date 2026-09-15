package com.marcmarco.game

import com.fasterxml.jackson.databind.ObjectMapper
import com.marcmarco.game.rawg.RawgGame
import com.marcmarco.game.rawg.RawgNamedItem
import com.marcmarco.game.rawg.RawgRestClient
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GameImportIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jwtEncoder: JwtEncoder

    @MockitoBean
    lateinit var rawgRestClient: RawgRestClient

    private val suffix = UUID.randomUUID().toString().substring(0, 8)

    private var authToken: String? = null

    private fun token(): String {
        authToken?.let { return it }
        return jwtEncoder.testToken().also { authToken = it }
    }

    private fun rawgGames(): List<RawgGame> = listOf(
        RawgGame(
            name = "Importado_$suffix",
            released = "2020-05-10",
            background_image = "https://img/uno.jpg",
            genres = listOf(RawgNamedItem("Action")),
            developers = listOf(RawgNamedItem("DevA")),
            publishers = listOf(RawgNamedItem("PubA")),
        ),
        RawgGame(
            name = "SinFecha_$suffix",
            released = "TBA",
            genres = emptyList(),
        ),
    )

    @Test
    fun `importar crea los juegos y devuelve el resumen`() {
        given(rawgRestClient.fetchGames(anyInt())).willReturn(rawgGames())

        mockMvc.perform(post("/api/games/import").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imported").value(2))
            .andExpect(jsonPath("$.skipped").value(0))

        mockMvc.perform(get("/api/games").param("name", "Importado_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Importado_$suffix"))
            .andExpect(jsonPath("$[0].genre").value("Action"))
            .andExpect(jsonPath("$[0].releaseDate").value("2020-05-10"))
            .andExpect(jsonPath("$[0].developer").value("DevA"))
            .andExpect(jsonPath("$[0].publisher").value("PubA"))
            .andExpect(jsonPath("$[0].cover").value("https://img/uno.jpg"))
    }

    @Test
    fun `importar dos veces no duplica los juegos`() {
        given(rawgRestClient.fetchGames(anyInt())).willReturn(rawgGames())

        mockMvc.perform(post("/api/games/import").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imported").value(2))
            .andExpect(jsonPath("$.skipped").value(0))

        mockMvc.perform(post("/api/games/import").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imported").value(0))
            .andExpect(jsonPath("$.skipped").value(2))
    }

    @Test
    fun `fecha TBA se guarda como nula`() {
        given(rawgRestClient.fetchGames(anyInt())).willReturn(rawgGames())

        mockMvc.perform(post("/api/games/import").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/games").param("name", "SinFecha_$suffix"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].releaseDate").value(org.hamcrest.Matchers.nullValue()))
    }

    @Test
    fun `importar sin autenticacion devuelve 401`() {
        mockMvc.perform(post("/api/games/import"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `count fuera de rango devuelve 400`() {
        mockMvc.perform(post("/api/games/import").header(HttpHeaders.AUTHORIZATION, "Bearer ${token()}").param("count", "101"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("count debe estar entre 1 y 100"))
    }
}