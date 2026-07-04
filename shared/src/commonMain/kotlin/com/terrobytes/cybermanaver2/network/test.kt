package com.terrobytes.cybermanaver2.network

fun main() {
    println("Debut")
    val client = MikrotikRawClient("192.168.88.1")
    println(client.login("admin", "malchance"))
    println(client.execute("/system/resource/print"))
}

fun testPort(ip: String) {
    val socket = java.net.Socket(ip, 8728)
    println("Connected OK")
    socket.close()
}