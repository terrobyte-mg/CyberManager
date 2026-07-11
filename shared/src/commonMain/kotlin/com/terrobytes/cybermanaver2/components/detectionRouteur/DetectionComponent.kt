package com.terrobytes.cybermanaver2.components.detectionRouteur

import com.arkivanov.decompose.value.Value
import com.terrobytes.cybermanaver2.models.RouterUiState
import com.terrobytes.cybermanaver2.models.Routeur

interface DetectionComponent {

    val state : Value<RouterUiState>

    fun startScan()
    fun selectRouter(routeur: Routeur)
    fun connect()
    fun connectManually()
    fun testConnection(ip: String, username: String, password: String)

}