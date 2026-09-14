package com.marcmarco.frontend.ui

sealed interface MainSection {
    data object Home : MainSection
    data object Users : MainSection
    data object Games : MainSection
    data object Profile : MainSection
    data class UserDetail(val userId: Long) : MainSection
    data class GameDetail(val gameId: Long) : MainSection
}