@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.terrobytes.cybermanaver2.storage

import android.content.Context
import java.io.File
import androidx.core.content.edit

actual class BackupStore actual constructor() {

    private val prefs by lazy {
        AppContextProvider.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val backupDir by lazy {
        File(AppContextProvider.context.filesDir, "backups").apply { mkdirs() }
    }

    private val exportFile get() = File(backupDir, "export.rsc")
    private val binaryFile get() = File(backupDir, "binary.backup")
    private val binaryPasswordFile = File(backupDir, "binary.password")

    actual fun saveBackup(backup: RouterBackup) {
        backupDir.mkdirs()
        exportFile.writeText(backup.textExport, Charsets.UTF_8)

        if (backup.binaryBackup != null) {
            binaryFile.writeBytes(backup.binaryBackup)
        } else if (binaryFile.exists()) {
            binaryFile.delete()
        }

        if (backup.binaryBackupPassword != null) {
            binaryFile.writeBytes(backup.binaryBackupPassword.toByteArray(Charsets.UTF_8))
        } else if (binaryPasswordFile.exists()) {
            binaryPasswordFile.delete()
        }

        prefs.edit {
            putString(KEY_IDENTITY, backup.routerIdentity)
                .putLong(KEY_TAKEN_AT, backup.takenAt)
        }
    }

    actual fun getBackup(): RouterBackup? {
        if (!exportFile.exists()) return null

        val identity = prefs.getString(KEY_IDENTITY, null) ?: return null
        val takenAt = prefs.getLong(KEY_TAKEN_AT, -1L).takeIf { it >= 0 } ?: return null

        return RouterBackup(
            textExport = exportFile.readText(Charsets.UTF_8),
            binaryBackup = if (binaryFile.exists()) binaryFile.readBytes() else null,
            binaryBackupPassword = if (binaryPasswordFile.exists()) {
                String(binaryFile.readBytes(), Charsets.UTF_8)
            } else null,
            routerIdentity = identity,
            takenAt = takenAt,
        )
    }

    actual fun clearBackup() {
        exportFile.delete()
        binaryFile.delete()
        binaryPasswordFile.delete()
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "cybermanager_backup"
        const val KEY_IDENTITY = "router_identity"
        const val KEY_TAKEN_AT = "taken_at"
    }
}