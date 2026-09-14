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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
fun GameDetailScreen(
    gameApi: GameApi,
    token: String,
    gameId: Long,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var game by remember { mutableStateOf<GameResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                game = gameApi.getGame(gameId, token)
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = "No se pudo conectar con el backend"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(gameId) { load() }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            error != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { load() }) { Text("Reintentar") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBack) { Text("Volver") }
            }

            else -> game?.let { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onBack) { Text("Volver") }
                        Spacer(Modifier.width(12.dp))
                        Text(current.name, style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.height(16.dp))
                    current.cover?.let {
                        Text(
                            text = "Portada: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    current.description?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                    }
                    DetailRow("Género", current.genre)
                    DetailRow("Desarrollador", current.developer)
                    DetailRow("Publisher", current.publisher)
                    current.releaseDate?.let { DetailRow("Fecha de lanzamiento", it) }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}