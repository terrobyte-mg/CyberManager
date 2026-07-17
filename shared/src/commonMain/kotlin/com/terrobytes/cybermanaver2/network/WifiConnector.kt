package com.terrobytes.cybermanaver2.network

sealed class WifiConnectionResult {
    /** Connection request succeeded (Android: user approved the system dialog). */
    data object Connected : WifiConnectionResult()
    data object Failed : WifiConnectionResult()
    /** Platform/OS version doesn't support programmatic Wi-Fi connection here. */
    data object Unsupported : WifiConnectionResult()
    data object TimedOut : WifiConnectionResult()
}

/**
 * Best-effort request for the device itself (not the router) to join a
 * given Wi-Fi network. Needed because after a reset+injection, the phone/PC
 * running the app is very likely still associated with the OLD network
 * config and won't reach the router until it rejoins the new SSID.
 *
 * Android: shows a system connect dialog the user must approve - this is an
 * OS restriction (API 29+) that can't be bypassed by a normal app.
 * Desktop: shells out to the OS's own Wi-Fi tool (nmcli/networksetup/netsh)
 * - best-effort, depends on what's installed and on permissions.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class WifiConnector(platformContext: PlatformContext?) {

    /** Checks whether Wi-Fi is enabled, and tries to enable it if not. Returns true if usable afterward. */
    suspend fun checkInterfaceStatus(): Boolean

    suspend fun connectToWifi(ssid: String, password: String, timeoutSeconds: Int = 30): WifiConnectionResult

    /** NetworkTarget to route API/FTP traffic through after a successful connect, if the platform can provide one. */
    fun lastConnectedNetworkTarget(): NetworkTarget?
}