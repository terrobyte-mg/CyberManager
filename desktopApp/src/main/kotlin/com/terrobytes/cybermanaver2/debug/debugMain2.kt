package com.terrobytes.cybermanaver2.debug

import com.terrobytes.cybermanaver2.network.MARKER_API_USERNAME
import com.terrobytes.cybermanaver2.network.PlatformContext
import com.terrobytes.cybermanaver2.network.RouterBackupManager
import com.terrobytes.cybermanaver2.network.RouterInjectionManager
import com.terrobytes.cybermanaver2.network.WifiConnector
import com.terrobytes.cybermanaver2.storage.BackupStore
import com.terrobytes.cybermanaver2.templates.CyberTemplateParams
import kotlinx.coroutines.runBlocking

/**
 * !!! DESTRUCTIVE - resets and reboots a real router !!!
 *
 * Only run this against test/lab hardware you can physically reach (reset
 * button) if the rollback pipeline doesn't behave as expected. Edit
 * HOST/USERNAME/PASSWORD below.
 */
private const val HOST = "192.168.88.1"
private const val USERNAME = "admin"
private const val PASSWORD = "brayan1234567"

private val TEMPLATE = CyberTemplateParams(
    ssid24 = "CyberManager-Test",
    ssid5 = "CyberManager-Test-5G",
    wifiPassword = "CyberTest1234",
)

fun main() = runBlocking {
    println("=== CyberManager reset+injection smoke test ===")
    println("Target: $USERNAME@$HOST")
    println()

    println("--- Step 1: backup (required, it's what the rollback replays) ---")
    val backupResult = RouterBackupManager().backupRouter(
        networkTarget = null,
        host = HOST,
        username = USERNAME,
        password = PASSWORD,
    ) { println("[backup] $it") }

    if (backupResult.isFailure) {
        println("ABORTING: backup failed, refusing to reset without one. ${backupResult.exceptionOrNull()}")
        return@runBlocking
    }
    println("Backup OK.")
    println()

    println("--- Step 2: upload injection script + trigger reset ---")
    val injectionManager = RouterInjectionManager()

    val uploadResult = injectionManager.uploadAndReset(
        host = HOST,
        adminUsername = USERNAME,
        adminPassword = PASSWORD,
        template = TEMPLATE,
        backupPassword = BackupStore().getBackup()?.binaryBackupPassword ?: "",
    ) { println("[inject] $it") }

    val apPassword = uploadResult.getOrElse {
        println("ABORTING: could not trigger reset. $it")
        return@runBlocking
    }
    println("Reset triggered. Marker account password: $apPassword")
    println("(save this somewhere - you'll want it to log in manually if the app-side reconnect fails)")
    println()

    println("--- Step 3: wait for reboot, reconnect, verify marker, cancel failsafe ---")
    val wifiConnector = WifiConnector(PlatformContext())

    val verifyResult = injectionManager.reconnectAndVerify(
        host = HOST,
        apPassword = apPassword,
        wifiConnector = wifiConnector,
        ssid = TEMPLATE.ssid24,
        wifiPassword = TEMPLATE.wifiPassword,
    ) { println("[verify] $it") }

    verifyResult.fold(
        onSuccess = { reachedHost ->
            println()
            println("SUCCESS - router reachable again at $reachedHost")
            println("Marker present, failsafe scheduler cancelled.")
            println("Marker account: $MARKER_API_USERNAME / $apPassword")
        },
        onFailure = { error ->
            println()
            println("DID NOT CONFIRM: ${error.message}")
            println("If the router truly never came back, its own scheduler will restore")
            println("the pre-injection config automatically once the failsafe interval elapses.")
        }
    )
}