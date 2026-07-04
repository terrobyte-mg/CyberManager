package com.terrobytes.cybermanaver2.models

data class Routeur(
    val ipAddress: String,
    val macAddress: String? = null,
    val name: String,
    val model: String? = null,
    val version: String? = null,
    val isOnline: Boolean = false,
    val source: RouterDiscoveryMethod
)
