package com.terrobytes.cybermanaver2.network

import com.terrobytes.cybermanaver2.templates.CyberTemplateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom
import kotlin.time.Duration.Companion.milliseconds

sealed class InjectionStep {
    data object UploadingScript : InjectionStep()
    data object TriggeringReset : InjectionStep()
    data object WaitingForReboot : InjectionStep()
    data object EnsuringWifiEnabled : InjectionStep()
    data object ConnectingToWifi : InjectionStep()
    data class Reconnecting(val attempt: Int) : InjectionStep()
    data object Verifying : InjectionStep()
    data object CancellingFailsafe : InjectionStep()
    data object Done : InjectionStep()
    data class Failed(val reason: String) : InjectionStep()
    /** Reached only if the app gave up reconnecting - the router's own scheduler is still armed and will roll back on its own. */
    data object GaveUpWaiting : InjectionStep()
}

private const val INJECT_SCRIPT_NAME = "cybermanager-inject.rsc"
private const val BACKUP_EXPORT_NAME = "cybermanager-backup.backup"
const val MARKER_API_USERNAME = "cybermanager-api"

class RouterInjectionManager {

    /**
     * Uploads the injection script and triggers the reset. Returns as soon as
     * the reset command has been sent - the router reboots from that point,
     * this function does NOT wait for it to come back. Call [reconnectAndVerify]
     * separately for that (the whole point of run-after-reset is the app may
     * lose network connectivity during the gap).
     *
     * Assumes a backup was already taken (see RouterBackupManager) so
     * [BACKUP_EXPORT_NAME] exists on the router's flash - that file is reused
     * directly as the rollback source, no separate upload needed since files
     * survive reset-configuration.
     */
    suspend fun uploadAndReset(
        host: String,
        adminUsername: String,
        adminPassword: String,
        template: CyberTemplateParams,
        backupPassword: String,
        onStep: (InjectionStep) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {

        val apPassword = generateRandomPassword()

        onStep(InjectionStep.UploadingScript)

        // Detect the persistent flash/ folder (see RouterFileSystem.kt) so the
        // injection script's own file, AND the rollback file it references,
        // both live somewhere that survives the reboot we're about to trigger.
        val flashPrefix = try {
            val probe = MikrotikRawClient(null, host)
            try {
                if (!probe.login(adminUsername, adminPassword)) {
                    val msg = "Authentification refusée"
                    onStep(InjectionStep.Failed(msg))
                    return@withContext Result.failure(IllegalStateException(msg))
                }
                detectFlashPrefix(probe)
            } finally {
                runCatching { probe.close() }
            }
        } catch (e: Exception) {
            val msg = "Connexion impossible: ${e.message ?: e.toString()}"
            onStep(InjectionStep.Failed(msg))
            return@withContext Result.failure(e)
        }

        val injectScriptName = "$flashPrefix$INJECT_SCRIPT_NAME"
        val rollbackFileName = "$flashPrefix$BACKUP_EXPORT_NAME"

        val script = InjectionScripts.buildCyberInjectionScript(
            params = template,
            apUsername = MARKER_API_USERNAME,
            apPassword = apPassword,
            rollbackFileName = rollbackFileName,
            rollbackPassword = backupPassword
        )

        val ftp = MikrotikFtpClient(host, adminUsername, adminPassword)
        try {
            if (!ftp.connect()) {
                val msg = "Connexion FTP impossible"
                onStep(InjectionStep.Failed(msg))
                return@withContext Result.failure(IllegalStateException(msg))
            }
            if (!ftp.uploadScript(injectScriptName, script)) {
                val msg = "Upload du script d'injection impossible"
                onStep(InjectionStep.Failed(msg))
                return@withContext Result.failure(IllegalStateException(msg))
            }
        } finally {
            ftp.disconnect()
        }

        onStep(InjectionStep.TriggeringReset)
        try {
            val client = MikrotikRawClient(null, host)
            try {
                if (!client.login(adminUsername, adminPassword)) {
                    val msg = "Authentification refusée avant le reset"
                    onStep(InjectionStep.Failed(msg))
                    return@withContext Result.failure(IllegalStateException(msg))
                }

                // This normally reboots the router immediately - if we get a
                // clean reply back instead of the connection dying, check it:
                // that means the router refused the command outright.
                val resetReply = runCatching {
                    client.execute(
                        listOf(
                            "/system/reset-configuration",
                            "=keep-users=yes",
                            "=no-defaults=yes",
                            "=run-after-reset=$injectScriptName",
                        )
                    )
                }.getOrNull()

                if (resetReply != null && isApiTrap(resetReply)) {
                    val msg = "Le routeur a refusé le reset: ${apiTrapMessage(resetReply)}"
                    onStep(InjectionStep.Failed(msg))
                    return@withContext Result.failure(IllegalStateException(msg))
                }
            } finally {
                runCatching { client.close() }
            }
        } catch (_: Exception) {
            // Expected: the connection dies as the router reboots.
        }

        onStep(InjectionStep.WaitingForReboot)
        Result.success(apPassword)
    }

    /**
     * Polls until the marker account (created by the injection script)
     * accepts a login and the marker note is present, or [timeoutSeconds]
     * elapses. Tries both [host] (in case the router kept its IP) and the
     * Mikrotik factory default 192.168.88.1 (in case reset restored it) on
     * every attempt.
     */
    suspend fun reconnectAndVerify(
        host: String,
        apPassword: String,
        wifiConnector: WifiConnector,
        ssid: String,
        wifiPassword: String,
        timeoutSeconds: Int = 150,
        pollIntervalSeconds: Int = 5,
        onStep: (InjectionStep) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {

        onStep(InjectionStep.EnsuringWifiEnabled)
        wifiConnector.checkInterfaceStatus()
        // Proceed regardless: on Android we may have only been able to prompt
        // the user rather than confirm it's on; on desktop the check itself
        // may be best-effort. Either way, trying to connect next is safe.
        // Same here: don't bail out on Failed/Unsupported/TimedOut - the
        // device might already be on the right network some other way (e.g.
        // the user reconnected manually), so we still fall through to polling.

        val networkTarget = wifiConnector.lastConnectedNetworkTarget()
        val candidateHosts = listOf(host, "192.168.88.1").distinct()
        var attempt = 0

        val reachedHost = withTimeoutOrNull((timeoutSeconds * 1000L).milliseconds) {
            while (true) {
                attempt++
                onStep(InjectionStep.Reconnecting(attempt))

                onStep(InjectionStep.ConnectingToWifi)
                val result = wifiConnector.connectToWifi(ssid, wifiPassword, timeoutSeconds = minOf(timeoutSeconds, 30))

                if (result == WifiConnectionResult.Connected) {
                    for (candidate in candidateHosts) {
                        if (tryVerifyOnce(candidate, apPassword, networkTarget, onStep)) {
                            return@withTimeoutOrNull candidate
                        }
                    }
                }

                delay((pollIntervalSeconds * 1000L).milliseconds)
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }

        if (reachedHost != null) {
            onStep(InjectionStep.Done)
            Result.success(reachedHost)
        } else {
            onStep(InjectionStep.GaveUpWaiting)
            Result.failure(
                IllegalStateException(
                    "Reconnexion impossible dans le délai imparti - le filet de sécurité du " +
                            "routeur va restaurer l'ancienne config automatiquement"
                )
            )
        }
    }

    private suspend fun tryVerifyOnce(
        host: String,
        apPassword: String,
        networkTarget: NetworkTarget?,
        onStep: (InjectionStep) -> Unit,
    ): Boolean {
        val client = try {
            MikrotikRawClient(networkTarget, host)
        } catch (_: Exception) {
            return false
        }

        try {
            if (!client.login(MARKER_API_USERNAME, apPassword)) return false

            onStep(InjectionStep.Verifying)
            val note = client.execute("/system/note/print")
            val noteValue = parseApiValue(note, "note") ?: ""
            if (!noteValue.contains("CyberManager")) return false

            onStep(InjectionStep.CancellingFailsafe)
            runCatching {
                client.execute(
                    listOf("/system/scheduler/remove", "=numbers=${InjectionScripts.FAILSAFE_SCHEDULER_NAME}")
                )
            }
            return true
        } finally {
            runCatching { client.close() }
        }
    }

    private fun generateRandomPassword(length: Int = 24): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%^&*"
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}