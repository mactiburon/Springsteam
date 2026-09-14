package com.marcmarco.frontend.ui

sealed interface Screen {
    data object Login : Screen
    data object Register : Screen
    data object Main : Screen
}