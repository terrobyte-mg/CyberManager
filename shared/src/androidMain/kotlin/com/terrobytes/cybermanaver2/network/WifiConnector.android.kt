@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.terrobytes.cybermanaver2.network

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import kotlin.time.Duration.Companion.milliseconds

actual class WifiConnector actual constructor(private val platformContext: PlatformContext?) {

    private var connectedTarget: NetworkTarget? = null

    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    actual suspend fun checkInterfaceStatus(): Boolean {
        val context = platformContext?.androidContext ?: return false
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false

        if (wifiManager.isWifiEnabled) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Apps can't toggle Wi-Fi programmatically since API 29 (OS
            // restriction, not a permission we're missing) - open the
            // quick-settings Wi-Fi panel so the user can flip it in one tap
            // without leaving the app. We can't await their action here.
            val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
            return false
        }

        @Suppress("DEPRECATION")
        return wifiManager.setWifiEnabled(true)
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    actual suspend fun connectToWifi(ssid: String, password: String, timeoutSeconds: Int): WifiConnectionResult {
        val context = platformContext?.androidContext ?: return WifiConnectionResult.Unsupported

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pre-API 29 would need the deprecated WifiManager.addNetwork/enableNetwork
            // path - not implemented, this app's minSdk should already be 29+.
            return WifiConnectionResult.Unsupported
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        // No NET_CAPABILITY_INTERNET requirement: this is a local router LAN,
        // it may well have no upstream internet yet at reconnect time.
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val network = withTimeoutOrNull((timeoutSeconds * 1000L).milliseconds) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        if (continuation.isActive) continuation.resumeWith(Result.success(network))
                    }

                    override fun onUnavailable() {
                        if (continuation.isActive) continuation.resumeWith(Result.success(null))
                    }
                }

                connectivityManager.requestNetwork(request, callback)

                continuation.invokeOnCancellation {
                    runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                }
            }
        }

        if (network == null) return WifiConnectionResult.TimedOut

        connectedTarget = buildNetworkTarget(connectivityManager, network, ssid)
        return WifiConnectionResult.Connected
    }

    actual fun lastConnectedNetworkTarget(): NetworkTarget? = connectedTarget

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun buildNetworkTarget(cm: ConnectivityManager, network: Network, ssid: String): NetworkTarget? {
        val properties = cm.getLinkProperties(network) ?: return null
        val ip = properties.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull()
            ?: return null
        val baseIp = ip.hostAddress?.substringBeforeLast(".") ?: return null
        return NetworkTarget(baseIp = baseIp, name = ssid, network = network)
    }
}