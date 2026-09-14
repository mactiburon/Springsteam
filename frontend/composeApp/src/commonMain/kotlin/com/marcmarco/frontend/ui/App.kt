package com.marcmarco.frontend.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.marcmarco.frontend.api.AuthApi
import com.marcmarco.frontend.api.dto.UserResponse
import com.marcmarco.frontend.ui.Screen.Login
import com.marcmarco.frontend.ui.Screen.Main
import com.marcmarco.frontend.ui.Screen.Register

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val authApi = remember { AuthApi() }
        var session by remember { mutableStateOf<Session?>(null) }
        var screen by remember { mutableStateOf<Screen>(Login) }

        when (screen) {
            Login -> LoginScreen(
                authApi = authApi,
                onLoggedIn = {
                    session = it
                    screen = Main
                },
                onGoToRegister = { screen = Register },
            )

            Register -> RegisterScreen(
                authApi = authApi,
                onLoggedIn = {
                    session = it
                    screen = Main
                },
                onGoToLogin = { screen = Login },
            )

            Main -> session?.let { current ->
                MainContent(
                    session = current,
                    onProfileUpdated = { updated: UserResponse -> session = session?.copy(user = updated) },
                    onLogout = {
                        session = null
                        screen = Login
                    },
                )
            }
        }
    }
}