// RouterBackupManager.kt
package com.terrobytes.cybermanaver2.network

import com.terrobytes.cybermanaver2.storage.BackupStore
import com.terrobytes.cybermanaver2.storage.RouterBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

sealed class BackupStep {
    data object Connecting : BackupStep()
    data object ReadingIdentity : BackupStep()
    data object ExportingConfig : BackupStep()
    data object SavingBinaryBackup : BackupStep()
    data object DownloadingFiles : BackupStep()
    data object Persisting : BackupStep()
    data object Done : BackupStep()
    data class Failed(val reason: String) : BackupStep()
}

private const val BACKUP_BASE_NAME = "cybermanager-backup"

class RouterBackupManager(
    private val backupStore: BackupStore = BackupStore(),
) {

    /**
     * Réutilise le client déjà connecté dans [sessionManager] (ouvert au
     * login) au lieu d'en ouvrir un nouveau. FTP reste une connexion à part.
     */
    suspend fun backupRouter(
        host: String,
        username: String,
        password: String,
        sessionManager: MikrotikSessionManager,
        onStep: (BackupStep) -> Unit,
    ): Result<RouterBackup> = withContext(Dispatchers.IO) {

        onStep(BackupStep.Connecting)

        val client = sessionManager.client.value
            ?: run {
                val msg = "Aucune session active vers le routeur"
                onStep(BackupStep.Failed(msg))
                return@withContext Result.failure(IllegalStateException(msg))
            }

        try {
            onStep(BackupStep.ReadingIdentity)
            val identity = readIdentity(client) ?: "routeur-inconnu"

            val flashPrefix = detectFlashPrefix(client)
            val backupFileBase = "$flashPrefix$BACKUP_BASE_NAME"

            val backupPassword = generateBackupPassword()

            onStep(BackupStep.ExportingConfig)
            runCommand(client, listOf("/export", "=file=$backupFileBase"))

            onStep(BackupStep.SavingBinaryBackup)
            runCommand(client, listOf("/system/backup/save", "=name=$backupFileBase", "=password=$backupPassword"))

            onStep(BackupStep.DownloadingFiles)
            val (exportText, binaryBytes) = downloadBackupFiles(host, username, password, backupFileBase)

            onStep(BackupStep.Persisting)
            val backup = RouterBackup(
                textExport = exportText,
                binaryBackup = binaryBytes,
                binaryBackupPassword = backupPassword,
                routerIdentity = identity,
                takenAt = System.currentTimeMillis(),
            )
            backupStore.saveBackup(backup)

            onStep(BackupStep.Done)
            Result.success(backup)

        } catch (e: Exception) {
            onStep(BackupStep.Failed(e.message ?: e.toString()))
            Result.failure(e)
        }
    }

    private suspend fun downloadBackupFiles(
        host: String,
        username: String,
        password: String,
        fileBase: String,
    ): Pair<String, ByteArray?> {
        val ftp = MikrotikFtpClient(host, username, password)
        try {
            if (!ftp.connect()) {
                throw IllegalStateException("Connexion FTP impossible")
            }

            val exportBytes = ftp.downloadFile("$fileBase.rsc")
                ?: throw IllegalStateException("Fichier d'export introuvable sur le routeur")

            val binaryBytes = ftp.downloadFile("$fileBase.backup")

            return exportBytes.toString(Charsets.UTF_8) to binaryBytes
        } finally {
            ftp.disconnect()
        }
    }

    private fun readIdentity(client: MikrotikRawClient): String? {
        val raw = client.execute("/system/identity/print")
        return parseApiValue(raw, "name")
    }

    private fun runCommand(client: MikrotikRawClient, words: List<String>): String {
        val reply = client.execute(words)
        if (isApiTrap(reply)) {
            throw IllegalStateException("Commande refusée (${words.firstOrNull()}): ${apiTrapMessage(reply)}")
        }
        return reply
    }

    private fun generateBackupPassword(length: Int = 24): String {
        val chars = "ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghijklmopqrstuvwxyz23456789"
        val random = SecureRandom()
        return (1 .. length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}