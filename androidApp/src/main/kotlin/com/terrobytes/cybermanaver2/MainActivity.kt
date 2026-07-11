package com.terrobytes.cybermanaver2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.arkivanov.decompose.defaultComponentContext
import com.terrobytes.cybermanaver2.network.InfoNetworkDevice
import com.terrobytes.cybermanaver2.network.PlatformContext
import com.terrobytes.cybermanaver2.root.DefaultRootComponent
import com.terrobytes.cybermanaver2.root.RootComponent
import com.terrobytes.cybermanaver2.storage.AppContextProvider

class MainActivity : ComponentActivity() {

    private lateinit var root : RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        AppContextProvider.init(this)
        InfoNetworkDevice.initialize(PlatformContext(this))

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = Color.Transparent.toArgb()
            )
        )
        super.onCreate(savedInstanceState)

        root = DefaultRootComponent(componentContext = defaultComponentContext())

        setContent {
            App(root = root)
        }
    }
}