package com.marcmarco.frontend

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.marcmarco.frontend.ui.App

fun main() = application {
    val state = rememberWindowState(width = 480.dp, height = 680.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "SpringBootDemo",
        state = state,
    ) {
        App()
    }
}