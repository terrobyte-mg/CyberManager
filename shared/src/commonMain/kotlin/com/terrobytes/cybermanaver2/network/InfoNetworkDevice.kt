package com.terrobytes.cybermanaver2.network

object InfoNetworkDevice {

    fun getLocalIpAddress(): List<String> {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        val listAddresses = ArrayList<String>()

        for (intf in interfaces) {
            println(intf)
            if (intf.isUp && !intf.isLoopback && !intf.isVirtual) {
                println(intf.parent)
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        listAddresses.add(addr.hostAddress)
                    }
                }
            }
        }
        return listAddresses
    }

    fun getNetworkBaseIp(): List<String> {
        val ip = getLocalIpAddress()
        return ip.map { it.substringBeforeLast(".")}
    }

}