package com.terrobytes.cybermanaver2.network

import com.terrobytes.cybermanaver2.templates.CyberTemplateParams

/**
 * Builds the .rsc script executed locally on the router via `run-after-reset`.
 * `run-after-reset` implies a defaults-free reset regardless of the
 * `no-defaults` parameter, so this script defines EVERYTHING itself: bridge,
 * DHCP, DNS, firewall, wireless. Nothing is inherited from RouterOS' own
 * default-configuration.
 */
object InjectionScripts {

    const val FAILSAFE_SCHEDULER_NAME = "cybermanager-failsafe"

    fun buildCyberInjectionScript(
        params: CyberTemplateParams,
        apUsername: String,
        apPassword: String,
        rollbackFileName: String,
        rollbackPassword: String,
        failsafeMinutes: Int = 5,
    ): String = buildString {
        appendLine(":delay 15s")
        appendLine()
        appendWifiWaitLoop()
        appendLine()
        appendNetworkSetup(params)
        appendLine()
        appendWirelessSetup(params)
        appendLine()
        appendMarker(apUsername, apPassword)
        appendLine()
        appendFailsafe(rollbackFileName, rollbackPassword, failsafeMinutes)
    }

    private fun StringBuilder.appendWifiWaitLoop() {
        appendLine("# --- Attendre que les interfaces wifi soient pretes (jusqu'a 30s de plus) ---")
        appendLine(":local wifiWaitCount 0")
        appendLine("while (\$wifiWaitCount < 30 and [:len [/interface wireless find]] = 0) do={")
        appendLine("    :delay 1")
        appendLine("    :set wifiWaitCount (\$wifiWaitCount + 1)")
        appendLine("}")
    }

    private fun StringBuilder.appendNetworkSetup(p: CyberTemplateParams) {
        val adminStart = "${p.lanBase}.2"
        val adminEnd = "${p.lanBase}.${1 + p.adminCount}"
        val networkAddr = "${p.lanBase}.0"

        appendLine("# --- Bridge LAN ---")
        appendLine(":if ([:len [/interface bridge find name=bridge]] = 0) do={")
        appendLine("    /interface bridge add name=bridge")
        appendLine("}")
        appendLine(":if ([:len [/interface list find name=WAN]] = 0) do={")
        appendLine("    /interface list add name=WAN")
        appendLine("}")
        appendLine(":if ([:len [/interface list find name=LAN]] = 0) do={")
        appendLine("    /interface list add name=LAN")
        appendLine("}")
        appendLine(":if ([:len [/interface list member find interface=bridge list=LAN]] = 0) do={")
        appendLine("    /interface list member add interface=bridge list=LAN")
        appendLine("}")
        appendLine(":if ([:len [/interface list member find interface=ether1 list=WAN]] = 0) do={")
        appendLine("    /interface list member add interface=ether1 list=WAN")
        appendLine("}")
        appendLine()
        appendLine(":foreach lanPort in={\"ether2\";\"ether3\";\"ether4\";\"ether5\"} do={")
        appendLine("    :if ([:len [/interface find name=\$lanPort]] > 0 and [:len [/interface bridge port find interface=\$lanPort]] = 0) do={")
        appendLine("        /interface bridge port add bridge=bridge interface=\$lanPort")
        appendLine("    }")
        appendLine("}")
        appendLine()

        appendLine("# --- Adressage + DHCP ---")
        appendLine(":if ([:len [/ip address find interface=bridge]] = 0) do={")
        appendLine("    /ip address add address=${p.routerIp}/24 interface=bridge network=$networkAddr")
        appendLine("}")
        appendLine(":if ([:len [/ip pool find name=cyber-dhcp-pool]] = 0) do={")
        appendLine("    /ip pool add name=cyber-dhcp-pool ranges=${p.dhcpPoolStart}-${p.dhcpPoolEnd}")
        appendLine("}")
        appendLine(":if ([:len [/ip dhcp-server find name=cyber-dhcp]] = 0) do={")
        appendLine("    /ip dhcp-server add address-pool=cyber-dhcp-pool interface=bridge name=cyber-dhcp disabled=no")
        appendLine("}")
        appendLine(":if ([:len [/ip dhcp-server network find address=${p.lanCidr}]] = 0) do={")
        appendLine("    /ip dhcp-server network add address=${p.lanCidr} dns-server=${p.routerIp} gateway=${p.routerIp}")
        appendLine("}")
        appendLine(":if ([:len [/ip dhcp-client find interface=ether1]] = 0) do={")
        appendLine("    /ip dhcp-client add interface=ether1 disabled=no")
        appendLine("}")
        appendLine()

        appendLine("# --- DNS ---")
        appendLine("/ip dns set allow-remote-requests=yes")
        appendLine(":if ([:len [/ip dns static find name=router.lan]] = 0) do={")
        appendLine("    /ip dns static add name=router.lan address=${p.routerIp}")
        appendLine("}")
        appendLine()

        appendLine("# --- Listes d'adresses ---")
        appendLine(":if ([:len [/ip firewall address-list find list=cyber_admins]] = 0) do={")
        appendLine(
            "    /ip firewall address-list add list=cyber_admins address=$adminStart-$adminEnd " +
                    "comment=\"Postes admin reserves\""
        )
        appendLine("}")
        appendLine(
            "# cyber_allowed est peuplee/depeuplee dynamiquement par l'app (gestion du temps " +
                    "de connexion) - vide par defaut, donc pas d'acces internet tant que rien n'est ajoute."
        )
        appendLine()

        appendLine("# --- Pare-feu : input (vers le routeur lui-meme) ---")
        appendLine("# Note: pas idempotent (re-jouer ce script plusieurs fois sur la meme config duplique ces regles -")
        appendLine("# sans consequence fonctionnelle, juste cosmetique. Un vrai reset repart d'une base vide de toute facon.")
        appendLine(
            "/ip firewall filter add action=accept chain=input connection-state=established,related,untracked " +
                    "comment=\"Connexions existantes\""
        )
        appendLine("/ip firewall filter add action=drop chain=input connection-state=invalid comment=\"Invalide\"")
        appendLine("/ip firewall filter add action=accept chain=input protocol=icmp comment=\"ICMP\"")
        appendLine(
            "/ip firewall filter add action=accept chain=input src-address-list=cyber_admins " +
                    "comment=\"Admin -> routeur (API/WinBox)\""
        )
        appendLine(
            "/ip firewall filter add action=drop chain=input in-interface-list=!LAN " +
                    "comment=\"Rien depuis le WAN vers le routeur\""
        )
        appendLine()

        appendLine("# --- Pare-feu : forward (LAN <-> internet) ---")
        appendLine(
            "/ip firewall filter add action=accept chain=forward connection-state=established,related,untracked " +
                    "comment=\"Connexions existantes\""
        )
        appendLine("/ip firewall filter add action=drop chain=forward connection-state=invalid comment=\"Invalide\"")
        appendLine(
            "/ip firewall filter add action=fasttrack-connection chain=forward " +
                    "connection-state=established,related comment=\"Fasttrack\""
        )
        appendLine(
            "/ip firewall filter add action=accept chain=forward src-address-list=cyber_admins " +
                    "comment=\"Admin -> internet, acces complet\""
        )
        appendLine(
            "/ip firewall filter add action=accept chain=forward src-address-list=cyber_allowed " +
                    "comment=\"Client avec temps actif -> internet\""
        )
        appendLine(
            "/ip firewall filter add action=drop chain=forward dst-port=443 protocol=udp " +
                    "comment=\"Bloque QUIC (force fallback TCP, controle/logging plus fiable)\""
        )
        appendLine(
            "/ip firewall filter add action=drop chain=forward connection-nat-state=!dstnat " +
                    "connection-state=new in-interface-list=WAN comment=\"Rien d'entrant non sollicite depuis le WAN\""
        )
        appendLine(
            "/ip firewall filter add action=drop chain=forward " +
                    "comment=\"Deny by default: pas de temps actif = pas d'internet\""
        )
        appendLine()

        appendLine(":if ([:len [/ip firewall nat find out-interface-list=WAN action=masquerade]] = 0) do={")
        appendLine("    /ip firewall nat add action=masquerade chain=srcnat out-interface-list=WAN comment=\"NAT sortie internet\"")
        appendLine("}")
        appendLine()

        appendLine("# --- Acces distant restreint au LAN ---")
        appendLine("/tool mac-server set allowed-interface-list=LAN")
        appendLine("/tool mac-server mac-winbox set allowed-interface-list=LAN")
    }

    private fun StringBuilder.appendWirelessSetup(p: CyberTemplateParams) {
        appendLine("# --- Wi-Fi (bascule 2.4 / 5GHz selon la bande de chaque interface, rattachement au bridge) ---")
        appendLine(":foreach wIface in=[/interface wireless find] do={")
        appendLine("    :local band [/interface wireless get \$wIface band]")
        appendLine("    /interface bridge port add bridge=bridge interface=\$wIface")
        appendLine("    :if ([:find \$band \"2ghz\"] >= 0) do={")
        appendLine("        /interface wireless set \$wIface mode=ap-bridge disabled=no ssid=\"${escape(p.ssid24)}\"")
        appendLine("    } else={")
        appendLine("        /interface wireless set \$wIface mode=ap-bridge disabled=no ssid=\"${escape(p.ssid5)}\"")
        appendLine("    }")
        appendLine("}")
        appendLine()
        appendLine("/interface wireless security-profiles set [find default=yes] \\")
        appendLine("    mode=dynamic-keys authentication-types=wpa2-psk \\")
        appendLine("    wpa2-pre-shared-key=\"${escape(p.wifiPassword)}\"")
        appendLine(":foreach wIface in=[/interface wireless find] do={")
        appendLine("    /interface wireless set \$wIface security-profile=[/interface wireless security-profiles get [find default=yes] name]")
        appendLine("}")
    }

    private fun StringBuilder.appendMarker(apUsername: String, apPassword: String) {
        appendLine("# --- Marqueur : compte API dedie + note ---")
        appendLine(
            "/user group add name=cybermanager-api " +
                    "policy=api,read,write,test,winbox,password,!local,!telnet,!ssh,!ftp,!reboot,!policy,!sensitive,!romon"
        )
        appendLine("/user add name=\"${escape(apUsername)}\" group=cybermanager-api password=\"${escape(apPassword)}\"")
        appendLine("/system note set note=\"Configure par CyberManager\" show-at-login=no")
    }

    private fun StringBuilder.appendFailsafe(
        rollbackFileName: String,
        rollbackPassword: String,
        failsafeMinutes: Int,
    ) {
        val sanitizedFileName = if (rollbackFileName.endsWith(".backup")) rollbackFileName else "$rollbackFileName.backup"

        appendLine("# --- Filet de securite : script et scheduler avec politique 'test' ---")

        // 1. On crée d'abord le script de restauration (avec la politique 'test')
        appendLine("/system script add name=\"run-restore\" \\")
        appendLine("    policy=read,write,reboot,password,sensitive,policy,test \\")
        appendLine("    source=\"/system backup load name=\\\"$sanitizedFileName\\\" password=\\\"${escape(rollbackPassword)}\\\"\"")
        appendLine()

        // 2. On crée le scheduler qui exécute ce script après le délai imparti
        appendLine("/system scheduler add name=$FAILSAFE_SCHEDULER_NAME \\")
        appendLine("    interval=${failsafeMinutes}m \\")
        appendLine("    policy=read,write,reboot,password,sensitive,policy,test \\")
        appendLine("    on-event=\"/system script run run-restore\"")
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}