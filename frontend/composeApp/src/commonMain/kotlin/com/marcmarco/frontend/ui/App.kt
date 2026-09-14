package com.marcmarco.frontend.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.marcmarco.frontend.api.AuthApi

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val authApi = remember { AuthApi() }
        var session by remember { mutableStateOf<Session?>(null) }
        var screen by remember { mutableStateOf<Screen>(Screen.Login) }

        when (screen) {
            Screen.Login -> LoginScreen(
                authApi = authApi,
                onLoggedIn = {
                    session = it
                    screen = Screen.Home
                },
                onGoToRegister = { screen = Screen.Register },
            )

            Screen.Register -> RegisterScreen(
                authApi = authApi,
                onLoggedIn = {
                    session = it
                    screen = Screen.Home
                },
                onGoToLogin = { screen = Screen.Login },
            )

            Screen.Home -> HomeScreen(
                session = session,
                onLogout = {
                    session = null
                    screen = Screen.Login
                },
            )
        }
    }
}