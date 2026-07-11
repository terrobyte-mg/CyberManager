@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.terrobytes.cybermanaver2.network

import java.net.Socket

expect class PlatformContext

/**
 * A network interface found on the device (Wi-Fi, Ethernet...).
 * baseIp and name are common to every platform. Actuals are free to carry
 * extra platform-specific data alongside them (e.g. Android needs an
 * android.net.Network to bind sockets to a specific interface).
 */
expect class NetworkTarget {
    val baseIp: String
    val name: String
}

expect fun getNetworks(platformContext: PlatformContext?): List<NetworkTarget>
expect suspend fun isMikrotik(networkTarget: NetworkTarget, ip: String): Boolean

/**
 * Opens a TCP socket to [host]:[port]. When [networkTarget] is non-null and
 * the platform supports it (Android), the socket is bound to that specific
 * network interface - important on devices with several active networks
 * (Wi-Fi + mobile data) so traffic doesn't leak out the wrong one.
 * When null (manual connections, or a NetworkTarget that didn't survive
 * navigation), a plain unbound socket is used instead.
 */
expect fun openSocket(networkTarget: NetworkTarget?, host: String, port: Int, timeoutMs: Int = 5000): Socket