package com.terrobytes.cybermanaver2.network

import com.terrobytes.cybermanaver2.models.RouterDiscoveryMethod
import com.terrobytes.cybermanaver2.models.Routeur
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class WifiRouterScanner {

    suspend fun scanWifi(
        baseIp: String,
        onProgress: (Int) -> Unit
    ): List<Routeur> = coroutineScope {

        val results = Collections.synchronizedList(mutableListOf<Routeur>())
        val semaphore = Semaphore(50)
        val completed = AtomicInteger(0)

        val jobs = (1 .. 254).map { i ->

            val ip = "$baseIp.$i"

            async(Dispatchers.IO) {
                semaphore.withPermit {
                    if (isMikrotik(ip)) {
                        results.add(
                            Routeur(
                                ipAddress = ip,
                                name = "MikroTik",
                                isOnline = true,
                                source = RouterDiscoveryMethod.SCAN_WIFI
                            )
                        )
                    }

                    val progress = completed.incrementAndGet()
                    onProgress(progress)
                }
            }
        }

        jobs.awaitAll()
        results
    }

    private suspend fun isMikrotik(ip: String): Boolean {
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
}