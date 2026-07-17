package com.terrobytes.cybermanaver2.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformContext

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual data class NetworkTarget(
    actual val baseIp: String,
    actual val name: String,
)

actual fun getNetworks(platformContext: PlatformContext?): List<NetworkTarget> {

    val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

    val result = mutableListOf<NetworkTarget>()

    for (netInterface in interfaces) {

        if (!netInterface.isUp || netInterface.isLoopback) continue

        val ip = Collections.list(netInterface.inetAddresses)
            .filterIsInstance<Inet4Address>()
            .firstOrNull()
            ?: continue

        val baseIp = ip.hostAddress?.substringBeforeLast(".") ?: continue

        result.add(
            NetworkTarget(
                baseIp = baseIp,
                name = netInterface.displayName ?: netInterface.name,
            )
        )
    }

    return result.distinctBy { it.baseIp }
}

actual suspend fun isMikrotik(networkTarget: NetworkTarget, ip: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, 8291), 150)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}

actual fun openSocket(
    networkTarget: NetworkTarget?,
    host: String,
    port: Int,
    timeoutMs: Int,
    readTimeoutMs: Int,
): Socket {
    val socket = Socket()
    socket.connect(InetSocketAddress(host, port), timeoutMs)
    socket.soTimeout = readTimeoutMs
    return socket
}