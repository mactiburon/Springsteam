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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.marcmarco.frontend.api.UserApi
import com.marcmarco.frontend.api.dto.UpdateProfileRequest
import com.marcmarco.frontend.api.dto.UserResponse
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userApi: UserApi,
    session: Session,
    userId: Long,
    onProfileUpdated: (UserResponse) -> Unit,
    onBack: () -> Unit,
) {
    val isOwn = userId == session.user.id
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<UserResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsSuccess by remember { mutableStateOf(false) }

    var displayName by remember(userId) { mutableStateOf("") }
    var bio by remember(userId) { mutableStateOf("") }
    var avatar by remember(userId) { mutableStateOf("") }

    fun load() {
        scope.launch {
            loading = true
            error = null
            message = null
            try {
                val loaded = userApi.getUser(userId, session.token)
                user = loaded
                if (isOwn) {
                    displayName = loaded.displayName ?: ""
                    bio = loaded.bio ?: ""
                    avatar = loaded.avatar ?: ""
                }
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = "No se pudo conectar con el backend"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(userId) { load() }

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

            else -> user?.let { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onBack) { Text("Volver") }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isOwn) "Mi perfil" else "Perfil de @${current.username}",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = current.displayName ?: current.username,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "@${current.username}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(current.email, style = MaterialTheme.typography.bodyMedium)
                    current.bio?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    current.avatar?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("Avatar: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Registrado el ${current.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (isOwn) {
                        Spacer(Modifier.height(24.dp))
                        Text("Editar perfil", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Nombre visible (3-30)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio (máx 500)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = avatar,
                            onValueChange = { avatar = it },
                            label = { Text("Avatar URL (máx 300)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        message?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = it,
                                color = if (messageIsSuccess) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val cleanDisplayName = displayName.trim()
                                val cleanBio = bio.trim()
                                val cleanAvatar = avatar.trim()
                                val validation = when {
                                    cleanDisplayName.isNotEmpty() && cleanDisplayName.length < 3 ->
                                        "El nombre visible debe tener al menos 3 caracteres"
                                    cleanDisplayName.length > 30 ->
                                        "El nombre visible no puede superar 30 caracteres"
                                    cleanBio.length > 500 ->
                                        "La bio no puede superar 500 caracteres"
                                    cleanAvatar.length > 300 ->
                                        "El avatar no puede superar 300 caracteres"
                                    else -> null
                                }
                                if (validation != null) {
                                    message = validation
                                    messageIsSuccess = false
                                    return@Button
                                }
                                scope.launch {
                                    saving = true
                                    message = null
                                    try {
                                        val updated = userApi.updateProfile(
                                            token = session.token,
                                            request = UpdateProfileRequest(
                                                displayName = cleanDisplayName.ifBlank { null },
                                                bio = cleanBio.ifBlank { null },
                                                avatar = cleanAvatar.ifBlank { null },
                                            ),
                                        )
                                        user = updated
                                        displayName = updated.displayName ?: ""
                                        bio = updated.bio ?: ""
                                        avatar = updated.avatar ?: ""
                                        onProfileUpdated(updated)
                                        message = "Perfil actualizado"
                                        messageIsSuccess = true
                                    } catch (e: ApiException) {
                                        message = e.message
                                        messageIsSuccess = false
                                    } catch (e: Exception) {
                                        message = "No se pudo conectar con el backend"
                                        messageIsSuccess = false
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (saving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Guardar cambios")
                            }
                        }
                    }
                }
            }
        }
    }
}