package com.terrobytes.cybermanaver2.debug

import com.terrobytes.cybermanaver2.network.MikrotikRawClient
import com.terrobytes.cybermanaver2.network.RouterBackupManager
import com.terrobytes.cybermanaver2.storage.BackupStore
import kotlinx.coroutines.runBlocking
import java.net.Socket

/**
 * Manual smoke test against a REAL router - not a unit test, no assertions,
 * just prints what happens so you can eyeball it against your test hardware.
 *
 * Edit HOST/USERNAME/PASSWORD below, then run this file's main() directly
 * from the IDE (jvmMain target - this talks to a real router over your
 * machine's network, no Android device/emulator involved).
 *
 * Non-destructive: /export, /system backup save, and the FTP downloads only
 * read/create files, they don't touch any existing router config. Safe to
 * run repeatedly on the same router.
 */
private const val HOST = "192.168.88.1"
private const val USERNAME = "admin"
private const val PASSWORD = "brayan1234567"

fun main() = runBlocking {
//    val socket = Socket("192.168.88.1", 8728)


    val backupStore = BackupStore().getBackup()
    println(backupStore?.binaryBackupPassword)
//    try {
//        val client = MikrotikRawClient(
//            socket = socket,
//        )
//
//
//
//        client.login("admin", "brayan1234567")
//        val s = client.execute(listOf(
//            "/system/backup/load",
//            "=name=flash/cybermanager-backup.backup",
//            "=password=7Ek89LKUrg8qP2SAVOV6mZ7A"
//        ))
////        val s = client.execute("/system backup load name=flash/cybermanager-backup.backup password=1234")
//        println(s)
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
}