package com.terrobytes.cybermanaver2.network

import com.terrobytes.cybermanaver2.models.RouterDiscoveryMethod
import com.terrobytes.cybermanaver2.models.Routeur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * Scans a /24 subnet for Mikrotik devices.
 *
 * This used to be expect/actual with two near-identical implementations that
 * only differed by which `isMikrotik` they delegated to - and `isMikrotik` is
 * already its own expect/actual function. So this object doesn't need to be
 * platform-specific at all; it just calls the common `isMikrotik`.
 */
object WifiRouterScanner {

    suspend fun scanWifi(
        networkTarget: NetworkTarget,
        onProgress: (Int) -> Unit
    ): List<Routeur> = coroutineScope {

        val results = Collections.synchronizedList(mutableListOf<Routeur>())
        val semaphore = Semaphore(50)
        val completed = AtomicInteger(0)

        val jobs = (1..254).map { i ->
            val ip = "${networkTarget.baseIp}.$i"

            async(Dispatchers.IO) {
                semaphore.withPermit {
                    if (isMikrotik(networkTarget, ip)) {
                        results.add(
                            Routeur(
                                ipAddress = ip,
                                name = "Mikrotik",
                                isOnline = true,
                                source = RouterDiscoveryMethod.SCAN_WIFI,
                                networkTarget = networkTarget,
                            )
                        )
                    }
                    onProgress(completed.incrementAndGet())
                }
            }
        }

        jobs.awaitAll()
        results
    }
}