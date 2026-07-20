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
    data object GaveUpWaiting : InjectionStep()
}

private const val INJECT_SCRIPT_NAME = "cybermanager-inject.rsc"
private const val BACKUP_EXPORT_NAME = "cybermanager-backup.backup"
const val MARKER_API_USERNAME = "cybermanager-api"

class RouterInjectionManager {

    /**
     * Réutilise le client déjà présent dans [sessionManager] (celui ouvert au
     * login) au lieu d'ouvrir une nouvelle connexion API. FTP reste séparé
     * (protocole différent, pas de session à réutiliser).
     */
    suspend fun uploadAndReset(
        host: String,
        adminUsername: String,
        adminPassword: String,
        template: CyberTemplateParams,
        backupPassword: String?,
        sessionManager: MikrotikSessionManager,
        onStep: (InjectionStep) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {

        val apPassword = generateRandomPassword()

        onStep(InjectionStep.UploadingScript)

        val client = sessionManager.client.value
            ?: run {
                val msg = "Aucune session active vers le routeur"
                onStep(InjectionStep.Failed(msg))
                return@withContext Result.failure(IllegalStateException(msg))
            }

        val flashPrefix = try {
            detectFlashPrefix(client)
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
        } catch (_: Exception) {

        } finally {
            sessionManager.disconnect()
        }

        onStep(InjectionStep.WaitingForReboot)
        Result.success(apPassword)
    }

    suspend fun reconnectAndVerify(
        host: String,
        apPassword: String,
        wifiConnector: WifiConnector,
        ssid: String,
        wifiPassword: String,
        timeoutSeconds: Int = 150,
        pollIntervalSeconds: Int = 5,
        onStep: (InjectionStep) -> Unit,
        sessionManager: MikrotikSessionManager,
    ): Result<String> = withContext(Dispatchers.IO) {

        onStep(InjectionStep.EnsuringWifiEnabled)
        wifiConnector.checkInterfaceStatus()

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
                        if (tryVerifyOnce(candidate, apPassword, networkTarget, sessionManager, onStep)) {
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
        sessionManager: MikrotikSessionManager,
        onStep: (InjectionStep) -> Unit,
    ): Boolean {
        val client = try {
            MikrotikRawClient(networkTarget, host)
        } catch (_: Exception) {
            return false
        }

        try {
            if (!client.login(MARKER_API_USERNAME, apPassword)) {
                client.close()
                return false
            }

            onStep(InjectionStep.Verifying)
            val note = client.execute("/system/note/print")
            val noteValue = parseApiValue(note, "note") ?: ""
            if (!noteValue.contains("CyberManager")) {
                client.close()
                return false
            }

            onStep(InjectionStep.CancellingFailsafe)
            runCatching {
                client.execute(
                    listOf("/system/scheduler/remove", "=numbers=${InjectionScripts.FAILSAFE_SCHEDULER_NAME}")
                )
            }
            sessionManager.setClient(client)
            return true
        } catch (_: Exception) {
            client.close()
            return false
        }
    }

    private fun generateRandomPassword(length: Int = 24): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%^&*"
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}