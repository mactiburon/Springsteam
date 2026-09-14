package com.marcmarco.frontend.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcmarco.frontend.api.ApiException
import com.marcmarco.frontend.api.GameApi
import com.marcmarco.frontend.api.LibraryApi
import com.marcmarco.frontend.api.dto.GameResponse
import com.marcmarco.frontend.api.dto.LibraryAddRequest
import com.marcmarco.frontend.api.dto.LibraryResponse
import com.marcmarco.frontend.api.dto.LibraryUpdateRequest
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    libraryApi: LibraryApi,
    gameApi: GameApi,
    token: String,
    onOpenGame: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<LibraryResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var showAdd by remember { mutableStateOf(false) }
    var addQuery by remember { mutableStateOf("") }
    var addResults by remember { mutableStateOf<List<GameResponse>>(emptyList()) }
    var addLoading by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                entries = libraryApi.listLibrary(
                    token = token,
                    name = query.trim().ifBlank { null },
                    favoritesOnly = favoritesOnly,
                )
            } catch (e: ApiException) {
                error = e.message
                entries = emptyList()
            } catch (e: Exception) {
                error = "No se pudo conectar con el backend"
                entries = emptyList()
            } finally {
                loading = false
            }
        }
    }

    fun searchGames() {
        scope.launch {
            addLoading = true
            addError = null
            try {
                addResults = gameApi.listGames(token = token, name = addQuery.trim().ifBlank { null })
            } catch (e: ApiException) {
                addError = e.message
                addResults = emptyList()
            } catch (e: Exception) {
                addError = "No se pudo conectar con el backend"
                addResults = emptyList()
            } finally {
                addLoading = false
            }
        }
    }

    fun addGame(gameId: Long) {
        scope.launch {
            try {
                libraryApi.addToLibrary(token, LibraryAddRequest(gameId))
                showAdd = false
                addQuery = ""
                addResults = emptyList()
                load()
            } catch (e: ApiException) {
                addError = e.message
            } catch (e: Exception) {
                addError = "No se pudo conectar con el backend"
            }
        }
    }

    fun updateEntry(entry: LibraryResponse, isFavorite: Boolean? = null, hours: Double? = null) {
        scope.launch {
            try {
                val updated = libraryApi.updateEntry(
                    token = token,
                    gameId = entry.gameId,
                    request = LibraryUpdateRequest(isFavorite = isFavorite, hoursPlayed = hours),
                )
                entries = entries.map { if (it.gameId == updated.gameId) updated else it }
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = "No se pudo conectar con el backend"
            }
        }
    }

    fun removeEntry(gameId: Long) {
        scope.launch {
            try {
                libraryApi.removeFromLibrary(token, gameId)
                entries = entries.filter { it.gameId != gameId }
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = "No se pudo conectar con el backend"
            }
        }
    }

    LaunchedEffect(favoritesOnly) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Mi biblioteca", style = MaterialTheme.typography.headlineSmall)
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
            Button(onClick = { load() }) { Text("Buscar") }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = favoritesOnly,
                onClick = { favoritesOnly = !favoritesOnly },
                label = { Text("Solo favoritos") },
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showAdd = !showAdd }) {
                Text(if (showAdd) "Cancelar añadir" else "Añadir juego")
            }
        }
        if (showAdd) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = addQuery,
                    onValueChange = { addQuery = it },
                    label = { Text("Buscar juegos del catálogo") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { searchGames() }, enabled = addQuery.isNotBlank()) { Text("Buscar") }
            }
            if (addError != null) {
                Spacer(Modifier.height(8.dp))
                Text(addError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            if (addLoading) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (addResults.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    addResults.forEach { game ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(game.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = game.genre ?: "Sin género",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { addGame(game.id) }) { Text("Añadir") }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        when {
            loading -> {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)

            entries.isEmpty() -> Text(
                text = "Tu biblioteca está vacía",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn {
                items(entries, key = { it.gameId }) { entry ->
                    LibraryEntryRow(
                        entry = entry,
                        onOpenGame = { onOpenGame(entry.gameId) },
                        onToggleFavorite = {
                            updateEntry(entry, isFavorite = !entry.isFavorite)
                        },
                        onSaveHours = { updateEntry(entry, hours = it) },
                        onRemove = { removeEntry(entry.gameId) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryEntryRow(
    entry: LibraryResponse,
    onOpenGame: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSaveHours: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    var hoursText by remember(entry.gameId) { mutableStateOf(formatHours(entry.hoursPlayed)) }
    val parsedHours = hoursText.toDoubleOrNull()
    val validHours = parsedHours != null && parsedHours >= 0
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.game.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onOpenGame() }
                            .padding(vertical = 4.dp),
                    )
                    val meta = listOfNotNull(entry.game.genre, entry.game.developer).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = if (entry.isFavorite) "★" else "☆",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (entry.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.clickable { onToggleFavorite() }.padding(8.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatHours(entry.hoursPlayed)} h jugadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Horas") },
                    singleLine = true,
                    isError = hoursText.isNotBlank() && !validHours,
                    modifier = Modifier.width(120.dp),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSaveHours(parsedHours!!) }, enabled = validHours) {
                    Text("Guardar horas")
                }
            }
            Row {
                TextButton(onClick = onRemove) {
                    Text("Quitar de la biblioteca", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun formatHours(hours: Double): String =
    if (hours == hours.toLong().toDouble()) hours.toLong().toString() else hours.toString()