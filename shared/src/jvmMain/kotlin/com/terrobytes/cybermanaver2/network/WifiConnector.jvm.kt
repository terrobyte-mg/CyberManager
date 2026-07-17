package com.terrobytes.cybermanaver2.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class WifiConnector actual constructor(private val platformContext: PlatformContext?) {

    actual suspend fun checkInterfaceStatus(): Boolean = withContext(Dispatchers.IO) {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        try {
            when {
                os.contains("mac") -> ensureWifiEnabledMac()
                os.contains("nux") || os.contains("nix") -> ensureWifiEnabledLinux()
                os.contains("win") -> ensureWifiEnabledWindows()
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun ensureWifiEnabledLinux(): Boolean {
        if (runCommand("nmcli", "radio", "wifi").trim().equals("enabled", ignoreCase = true)) return true
        runCommand("nmcli", "radio", "wifi", "on")
        return runCommand("nmcli", "radio", "wifi").trim().equals("enabled", ignoreCase = true)
    }

    private fun ensureWifiEnabledMac(): Boolean {
        val device = detectMacWifiDevice() ?: "en0"
        if (runCommand("networksetup", "-getairportpower", device).contains("On", ignoreCase = true)) return true
        runCommand("networksetup", "-setairportpower", device, "on")
        return runCommand("networksetup", "-getairportpower", device).contains("On", ignoreCase = true)
    }

    /**
     * Best-effort only: unlike nmcli/networksetup, there's no single reliable
     * netsh command to flip the Wi-Fi radio on across all Windows versions
     * without knowing the exact adapter name and (usually) admin rights.
     * We can detect the state; we don't attempt to force it on.
     */
    private fun ensureWifiEnabledWindows(): Boolean {
        val output = runCommand("netsh", "wlan", "show", "interfaces")
        return output.contains("radio status", ignoreCase = true) && output.contains("on", ignoreCase = true)
    }

    private fun runCommand(vararg cmd: String): String {
        val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(10, TimeUnit.SECONDS)
        return output
    }

    actual suspend fun connectToWifi(ssid: String, password: String, timeoutSeconds: Int): WifiConnectionResult =
        withContext(Dispatchers.IO) {
            val os = System.getProperty("os.name")?.lowercase() ?: ""
            try {
                when {
                    os.contains("win") -> connectWindows(ssid, password, timeoutSeconds)
                    os.contains("mac") -> connectMac(ssid, password, timeoutSeconds)
                    os.contains("nux") || os.contains("nix") -> connectLinux(ssid, password, timeoutSeconds)
                    else -> WifiConnectionResult.Unsupported
                }
            } catch (_: Exception) {
                WifiConnectionResult.Failed
            }
        }

    // JVM's openSocket() ignores NetworkTarget entirely - no equivalent of
    // Android's network-binding need here, a plain reconnect attempt is enough.
    actual fun lastConnectedNetworkTarget(): NetworkTarget? = null

    private fun connectLinux(ssid: String, password: String, timeoutSeconds: Int): WifiConnectionResult {

        println("Connecting to $ssid...")
        println("Recherche de profile wifi existant")

        val research = ProcessBuilder("nmcli", "connection", "show", ssid)
            .redirectErrorStream(false)
            .start()

        val wifiExist = research.errorStream.bufferedReader().readText().isEmpty()
        research.destroyForcibly()

        if (!wifiExist) {

            println("Le profile n'existe pas")

            val process = ProcessBuilder("nmcli", "device", "wifi", "connect", ssid, "password", password)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                return WifiConnectionResult.TimedOut
            }

            return if (process.exitValue() == 0) WifiConnectionResult.Connected else WifiConnectionResult.Failed

        } else {

            println("Le profile existe")

            val process = ProcessBuilder("nmcli", "con", "up", "id", ssid)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return WifiConnectionResult.TimedOut
            }
            return if (process.exitValue() == 0) WifiConnectionResult.Connected else WifiConnectionResult.Failed
        }
    }

    private fun connectMac(ssid: String, password: String, timeoutSeconds: Int): WifiConnectionResult {
        val device = detectMacWifiDevice() ?: "en0"
        val process = ProcessBuilder("networksetup", "-setairportnetwork", device, ssid, password)
            .redirectErrorStream(true)
            .start()

        val finished = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return WifiConnectionResult.TimedOut
        }
        return if (process.exitValue() == 0) WifiConnectionResult.Connected else WifiConnectionResult.Failed
    }

    private fun detectMacWifiDevice(): String? {
        return try {
            val process = ProcessBuilder("networksetup", "-listallhardwareports").start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val lines = output.lines()
            val portIndex = lines.indexOfFirst { it.contains("Wi-Fi") || it.contains("AirPort") }
            if (portIndex == -1) return null
            lines.getOrNull(portIndex + 1)?.substringAfter("Device: ")?.trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun connectWindows(ssid: String, password: String, timeoutSeconds: Int): WifiConnectionResult {
        // netsh needs a saved profile before it can connect - build one as a
        // temp XML file, register it, then connect by name.
        val profileFile = File.createTempFile("cybermanager-wifi-", ".xml")
        try {
            profileFile.writeText(buildWindowsProfileXml(ssid, password))

            val addProcess = ProcessBuilder(
                "netsh", "wlan", "add", "profile", "filename=${profileFile.absolutePath}", "user=current"
            ).redirectErrorStream(true).start()
            addProcess.waitFor()
            if (addProcess.exitValue() != 0) return WifiConnectionResult.Failed

            val connectProcess = ProcessBuilder("netsh", "wlan", "connect", "name=$ssid", "ssid=$ssid")
                .redirectErrorStream(true)
                .start()

            val finished = connectProcess.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            if (!finished) {
                connectProcess.destroyForcibly()
                return WifiConnectionResult.TimedOut
            }
            return if (connectProcess.exitValue() == 0) WifiConnectionResult.Connected else WifiConnectionResult.Failed
        } finally {
            profileFile.delete()
        }
    }

    private fun buildWindowsProfileXml(ssid: String, password: String): String = """
        <?xml version="1.0"?>
        <WLANProfile xmlns="http://www.microsoft.com/networking/WLAN/profile/v1">
            <name>$ssid</name>
            <SSIDConfig>
                <SSID>
                    <name>$ssid</name>
                </SSID>
            </SSIDConfig>
            <connectionType>ESS</connectionType>
            <connectionMode>manual</connectionMode>
            <MSM>
                <security>
                    <authEncryption>
                        <authentication>WPA2PSK</authentication>
                        <encryption>AES</encryption>
                        <useOneX>false</useOneX>
                    </authEncryption>
                    <sharedKey>
                        <keyType>passPhrase</keyType>
                        <protected>false</protected>
                        <keyMaterial>$password</keyMaterial>
                    </sharedKey>
                </security>
            </MSM>
        </WLANProfile>
    """.trimIndent()
}