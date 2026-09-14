package com.marcmarco.frontend.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    session: Session,
    onOpenUsers: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Text(
                text = "Hola, ${session.user.displayName ?: session.user.username}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "@${session.user.username}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(session.user.email, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sesión JWT activa", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Expira en ${session.expiresIn / 3600} horas",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onOpenUsers, modifier = Modifier.fillMaxWidth()) {
                Text("Ver usuarios")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenGames, modifier = Modifier.fillMaxWidth()) {
                Text("Ver juegos")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth()) {
                Text("Mi perfil")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
        }
    }
}