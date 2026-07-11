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
class RouterBackup(
    val textExport: String,
    val binaryBackup: ByteArray?,
    val routerIdentity: String,
    val takenAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouterBackup) return false
        return textExport == other.textExport &&
                binaryBackup.contentEquals(other.binaryBackup) &&
                routerIdentity == other.routerIdentity &&
                takenAt == other.takenAt
    }

    override fun hashCode(): Int {
        var result = textExport.hashCode()
        result = 31 * result + (binaryBackup?.contentHashCode() ?: 0)
        result = 31 * result + routerIdentity.hashCode()
        result = 31 * result + takenAt.hashCode()
        return result
    }
}