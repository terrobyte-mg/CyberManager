package com.terrobytes.cybermanaver2

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.terrobytes.cybermanaver2.root.RootComponent
import com.terrobytes.cybermanaver2.root.RootContent

@Composable
fun App(root: RootComponent) {
    MaterialTheme {
        RootContent(component = root)
    }
}