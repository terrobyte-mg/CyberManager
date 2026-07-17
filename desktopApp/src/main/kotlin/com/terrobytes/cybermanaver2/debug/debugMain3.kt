package com.terrobytes.cybermanaver2.debug

import com.terrobytes.cybermanaver2.network.MikrotikRawClient
import java.net.Socket

fun main() {

    val socket = Socket("192.168.88.1", 8728)

    try {
        val client = MikrotikRawClient(
            socket = socket,
        )

        client.login("admin", "brayan1234567")

        val s = client.execute("/system backup load name=flash/cybermanager-backup.backup password=1234")
        println(s)
    } catch (e: Exception) {
        e.printStackTrace()
    }

}