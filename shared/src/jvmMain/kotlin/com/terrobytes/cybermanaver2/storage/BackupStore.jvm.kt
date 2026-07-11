package com.terrobytes.cybermanaver2.storage

import java.io.File
import java.util.Properties

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BackupStore actual constructor() {

    private val configDir = File(System.getProperty("user.home"), ".cybermanager")
    private val backupDir = File(configDir, "backups")
    private val metaFile = File(configDir, "backup.properties")
    private val exportFile = File(backupDir, "export.rsc")
    private val binaryFile = File(backupDir, "binary.backup")

    actual fun saveBackup(backup: RouterBackup) {
        backupDir.mkdirs()
        exportFile.writeBytes(JvmSecretBox.encrypt(backup.textExport.toByteArray(Charsets.UTF_8)))

        if (backup.binaryBackup != null) {
            binaryFile.writeBytes(JvmSecretBox.encrypt(backup.binaryBackup))
        } else if (binaryFile.exists()) {
            binaryFile.delete()
        }

        val props = Properties()
        props.setProperty(KEY_IDENTITY, backup.routerIdentity)
        props.setProperty(KEY_TAKEN_AT, backup.takenAt.toString())
        configDir.mkdirs()
        metaFile.outputStream().use { props.store(it, "CyberManager - dernier backup routeur") }
    }

    actual fun getBackup(): RouterBackup? {
        if (!metaFile.exists() || !exportFile.exists()) return null

        val props = Properties()
        metaFile.inputStream().use { props.load(it) }

        val identity = props.getProperty(KEY_IDENTITY) ?: return null
        val takenAt = props.getProperty(KEY_TAKEN_AT)?.toLongOrNull() ?: return null

        return RouterBackup(
            textExport = String(JvmSecretBox.decrypt(exportFile.readBytes()), Charsets.UTF_8),
            binaryBackup = if (binaryFile.exists()) JvmSecretBox.decrypt(binaryFile.readBytes()) else null,
            routerIdentity = identity,
            takenAt = takenAt,
        )
    }

    actual fun clearBackup() {
        exportFile.delete()
        binaryFile.delete()
        metaFile.delete()
    }

    private companion object {
        const val KEY_IDENTITY = "router_identity"
        const val KEY_TAKEN_AT = "taken_at"
    }
}