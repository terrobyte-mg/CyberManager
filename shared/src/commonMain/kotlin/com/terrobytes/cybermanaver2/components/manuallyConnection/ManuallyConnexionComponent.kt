package com.terrobytes.cybermanaver2.components.manuallyConnection

interface ManuallyConnexionComponent {
    fun onConnectClicked(ipAddress: String, username: String, password: String)
    fun onGoBackClicked()
}