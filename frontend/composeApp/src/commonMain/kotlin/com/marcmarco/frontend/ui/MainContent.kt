package com.marcmarco.frontend.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcmarco.frontend.api.GameApi
import com.marcmarco.frontend.api.UserApi
import com.marcmarco.frontend.api.dto.UserResponse

@Composable
fun MainContent(
    session: Session,
    onProfileUpdated: (UserResponse) -> Unit,
    onLogout: () -> Unit,
) {
    val userApi = remember { UserApi() }
    val gameApi = remember { GameApi() }
    var section by remember { mutableStateOf<MainSection>(MainSection.Home) }

    Scaffold(
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    BottomNavItem("Inicio", section == MainSection.Home) { section = MainSection.Home }
                    BottomNavItem("Usuarios", section == MainSection.Users) { section = MainSection.Users }
                    BottomNavItem("Juegos", section == MainSection.Games) { section = MainSection.Games }
                    BottomNavItem("Perfil", section == MainSection.Profile) { section = MainSection.Profile }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = section) {
                MainSection.Home -> HomeScreen(
                    session = session,
                    onOpenUsers = { section = MainSection.Users },
                    onOpenGames = { section = MainSection.Games },
                    onOpenProfile = { section = MainSection.Profile },
                    onLogout = onLogout,
                )

                MainSection.Users -> UsersScreen(
                    userApi = userApi,
                    token = session.token,
                    onOpenUser = { section = MainSection.UserDetail(it) },
                )

                MainSection.Games -> GamesScreen(
                    gameApi = gameApi,
                    token = session.token,
                    onOpenGame = { section = MainSection.GameDetail(it) },
                )

                MainSection.Profile -> ProfileScreen(
                    userApi = userApi,
                    session = session,
                    userId = session.user.id,
                    onProfileUpdated = onProfileUpdated,
                    onBack = { section = MainSection.Home },
                )

                is MainSection.UserDetail -> ProfileScreen(
                    userApi = userApi,
                    session = session,
                    userId = current.userId,
                    onProfileUpdated = onProfileUpdated,
                    onBack = { section = MainSection.Users },
                )

                is MainSection.GameDetail -> GameDetailScreen(
                    gameApi = gameApi,
                    token = session.token,
                    gameId = current.gameId,
                    onBack = { section = MainSection.Games },
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}