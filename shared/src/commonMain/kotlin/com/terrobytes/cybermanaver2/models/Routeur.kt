package com.terrobytes.cybermanaver2.models

import com.terrobytes.cybermanaver2.network.NetworkTarget

data class Routeur(
    val ssid24: String? = null,
    val ssid5: String? = null,
    val ipAddress: String,
    val macAddress: String? = null,
    val name: String,
    val model: String? = null,
    val version: String? = null,
    val isOnline: Boolean = false,
    val source: RouterDiscoveryMethod,
    val networkTarget: NetworkTarget?
)
