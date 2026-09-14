package com.marcmarco.frontend.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcmarco.frontend.api.ApiException
import com.marcmarco.frontend.api.GameApi
import com.marcmarco.frontend.api.dto.GameResponse
import kotlinx.coroutines.launch

@Composable
fun GamesScreen(
    gameApi: GameApi,
    token: String,
    onOpenGame: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var developer by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var releaseDateFrom by remember { mutableStateOf("") }
    var releaseDateTo by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var games by remember { mutableStateOf<List<GameResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search() {
        val q = query.trim().ifBlank { null }
        val g = genre.trim().ifBlank { null }
        val d = developer.trim().ifBlank { null }
        val p = publisher.trim().ifBlank { null }
        val from = releaseDateFrom.trim().ifBlank { null }
        val to = releaseDateTo.trim().ifBlank { null }
        scope.launch {
            loading = true
            error = null
            try {
                games = gameApi.listGames(
                    token = token,
                    name = q,
                    genre = g,
                    developer = d,
                    publisher = p,
                    releaseDateFrom = from,
                    releaseDateTo = to,
                )
            } catch (e: ApiException) {
                error = e.message
                games = emptyList()
            } catch (e: Exception) {
                error = "No se pudo conectar con el backend"
                games = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { search() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Juegos", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar por nombre") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { search() }) { Text("Buscar") }
        }
        TextButton(onClick = { showFilters = !showFilters }) {
            Text(if (showFilters) "Ocultar filtros" else "Más filtros")
        }
        if (showFilters) {
            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Género") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = developer,
                    onValueChange = { developer = it },
                    label = { Text("Desarrollador") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = { Text("Publisher") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = releaseDateFrom,
                    onValueChange = { releaseDateFrom = it },
                    label = { Text("Fecha desde (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = releaseDateTo,
                    onValueChange = { releaseDateTo = it },
                    label = { Text("Fecha hasta (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        when {
            loading -> {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)

            games.isEmpty() -> Text(
                text = "Sin resultados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn {
                items(games, key = { it.id }) { game ->
                    GameRow(game = game, onClick = { onOpenGame(game.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GameRow(game: GameResponse, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(game.name, style = MaterialTheme.typography.titleMedium)
            val meta = listOfNotNull(game.genre, game.developer, game.publisher).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}