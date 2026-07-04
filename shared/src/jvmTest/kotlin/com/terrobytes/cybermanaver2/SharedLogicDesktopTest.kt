package com.terrobytes.cybermanaver2

import com.terrobytes.cybermanaver2.network.MikrotikClient

fun main() {
    println("Debut")
    val client = MikrotikClient("192.168.88.1")

    val ok = client.connect()

    if (!ok) return

    val loginOk = client.login("admin", "malchance")

    if (!loginOk) return

    val result = client.execute("/system/resource/print")

    println(result)

    client.close()
}