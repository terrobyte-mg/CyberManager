package com.terrobytes.cybermanaver2.network

/**
 * Some Mikrotik devices (this hAP ac² included) keep persistent storage
 * under a separate "flash" folder; files written elsewhere sit on a RAM
 * drive that gets wiped on reboot/power-cycle. Anything that must survive a
 * reboot - the run-after-reset script, and the backup file it falls back to
 * - needs this prefix on devices where the folder exists.
 *
 * [client] must already be logged in.
 */
internal fun detectFlashPrefix(client: MikrotikRawClient): String {
    val reply = client.execute(listOf("/file/print", "?name=flash", "?type=disk"))
    return if (reply.contains("!re")) "flash/" else ""
}