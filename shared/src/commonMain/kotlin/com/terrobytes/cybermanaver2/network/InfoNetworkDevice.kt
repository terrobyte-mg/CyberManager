package com.terrobytes.cybermanaver2.network

object InfoNetworkDevice {

    private lateinit var platformContext: PlatformContext

    fun initialize(context: PlatformContext) {
        platformContext = context
    }

    fun getNetworks() : List<NetworkTarget> {
        return getNetworks(platformContext = platformContext)
    }

}