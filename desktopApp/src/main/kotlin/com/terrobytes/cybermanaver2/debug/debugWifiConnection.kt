package com.terrobytes.cybermanaver2.debug

import com.terrobytes.cybermanaver2.network.PlatformContext
import com.terrobytes.cybermanaver2.network.WifiConnectionResult
import com.terrobytes.cybermanaver2.network.WifiConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun main() {

//    val wifiConnector = WifiConnector(platformContext = PlatformContext())
//
//    val result = wifiConnector.connectToWifi(
//        ssid = "CyberManager-Test",
//        password = "CyberTest1234",
//        timeoutSeconds = 10
//    )
//
//    if (result == WifiConnectionResult.Connected) println("Reussis")
//    else println("Connect failed")

    val process = withContext(Dispatchers.IO) {
        ProcessBuilder("nmcli", "connection", "show", "CyberManager-Test")
            .redirectErrorStream(false)
            .start()
    }

    val result = process.errorStream.bufferedReader().readText()

    process.destroyForcibly()

    println(result.isEmpty())


}