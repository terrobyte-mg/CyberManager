package com.terrobytes.cybermanaver2.models

data class RouterUiState(
    val isScanning: Boolean = false,
    val progress: Int = 0,
    val total: Int = 254,
    val baseIp: String = "",
    val routers: List<Routeur> = emptyList(),
    val selectedRouter: Routeur? = null,
    val isConnecting: Boolean = false,
    val result: String = "",
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null
)