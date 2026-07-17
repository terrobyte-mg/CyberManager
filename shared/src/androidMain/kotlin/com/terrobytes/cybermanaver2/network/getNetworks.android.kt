@file:Suppress("DEPRECATION", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.terrobytes.cybermanaver2.network

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

actual class PlatformContext(val androidContext: Context)

actual data class NetworkTarget(
    actual val baseIp: String,
    actual val name: String,
    val network: Network,
)

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
actual fun getNetworks(
    platformContext: PlatformContext?
): List<NetworkTarget> {

    val context = platformContext?.androidContext ?: return emptyList()

    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val result = mutableListOf<NetworkTarget>()

    for (network in connectivityManager.allNetworks) {

        val properties = connectivityManager.getLinkProperties(network) ?: continue

        val ip = properties.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull()
            ?: continue

        val baseIp = ip.hostAddress?.substringBeforeLast(".") ?: continue

        result.add(
            NetworkTarget(
                baseIp = baseIp,
                name = properties.interfaceName ?: "unknown",
                network = network,
            )
        )
    }

    return result.distinctBy { it.baseIp }
}

actual suspend fun isMikrotik(networkTarget: NetworkTarget, ip: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            networkTarget.network.socketFactory.createSocket().use { socket ->
                socket.connect(InetSocketAddress(ip, 8291), 500)
            }
            true
        } catch (e: Exception) {
            Log.e("CyberManager", "Error connecting to ${networkTarget.name}", e)
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
    val socket = networkTarget?.network?.socketFactory?.createSocket() ?: Socket()
    socket.connect(InetSocketAddress(host, port), timeoutMs)
    socket.soTimeout = readTimeoutMs
    return socket
}