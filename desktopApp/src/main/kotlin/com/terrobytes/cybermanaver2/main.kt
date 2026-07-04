package com.terrobytes.cybermanaver2

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CyberManaver2",
    ) {
        App()
    }
}