package com.terrobytes.cybermanaver2.templates

/**
 * Parameters for the "cyber" (cyber-café) network template - the only
 * template available in v1. Every field has a sensible default so the setup
 * wizard can be skipped entirely and still produce a working config.
 */
data class CyberTemplateParams(
    val ssid24: String = "CyberManager-Test",
    val ssid5: String = "CyberManager-Test-5G",
    val wifiPassword: String = "CyberTest1234",
    /** Size of the reserved admin IP range (192.168.x.2 .. 192.168.x.(1+adminCount)). */
    val adminCount: Int = 3,
    /** Assumed /24. Router itself takes the .1 address. */
    val lanCidr: String = "192.168.88.0/24",
    val routerIp: String = "192.168.88.1",
    val dhcpPoolStart: String = "192.168.88.10",
    val dhcpPoolEnd: String = "192.168.88.254",
) {
    init {
        require(adminCount in 1..5) { "adminCount doit être entre 1 et 5" }
    }

    /** e.g. "192.168.88" from "192.168.88.0/24" - assumes a /24. */
    val lanBase: String get() = lanCidr.substringBefore("/").substringBeforeLast(".")
}