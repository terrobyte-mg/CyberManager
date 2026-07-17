package com.terrobytes.cybermanaver2.storage

/**
 * A snapshot of a router's configuration taken before we touch it.
 *
 * [textExport] is the output of `/export` - used to replay the previous
 * config back onto the router (no reboot needed, but fragile per MikroTik's
 * own community reports on complex configs).
 * [binaryBackup] is the raw contents of a `/system backup save` file,
 * downloaded off the router via FTP so it isn't only sitting on the device
 * we might be about to reset - closer to a "real" restore, but only usable
 * with `/system backup load` (reboot required, RouterOS-version sensitive).
 */
data class RouterBackup(
    val textExport: String,
    val binaryBackup: ByteArray?,
    val binaryBackupPassword: String?,
    val routerIdentity: String,
    val takenAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouterBackup

        if (takenAt != other.takenAt) return false
        if (textExport != other.textExport) return false
        if (!binaryBackup.contentEquals(other.binaryBackup)) return false
        if (binaryBackupPassword != other.binaryBackupPassword) return false
        if (routerIdentity != other.routerIdentity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = takenAt.hashCode()
        result = 31 * result + textExport.hashCode()
        result = 31 * result + (binaryBackup?.contentHashCode() ?: 0)
        result = 31 * result + binaryBackupPassword.hashCode()
        result = 31 * result + routerIdentity.hashCode()
        return result
    }
}