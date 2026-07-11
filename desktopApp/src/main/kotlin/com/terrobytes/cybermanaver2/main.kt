package com.terrobytes.cybermanaver2

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.terrobytes.cybermanaver2.network.InfoNetworkDevice
import com.terrobytes.cybermanaver2.network.PlatformContext
import com.terrobytes.cybermanaver2.root.DefaultRootComponent

fun main() = application {

    InfoNetworkDevice.initialize(PlatformContext())

    val lifecycle = LifecycleRegistry()
    val root = DefaultRootComponent(DefaultComponentContext(lifecycle = lifecycle))
    val windowState = rememberWindowState()

    LifecycleController(lifecycle, windowState)

    Window(
        onCloseRequest = ::exitApplication,
        title = "CyberManager",
        state = windowState,
    ) {
        App(root)
    }
}