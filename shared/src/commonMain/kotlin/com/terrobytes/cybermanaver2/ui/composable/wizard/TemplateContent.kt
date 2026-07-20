package com.terrobytes.cybermanaver2.ui.composable.wizard


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.terrobytes.cybermanaver2.templates.CyberTemplateParams
import com.terrobytes.cybermanaver2.ui.composable.colors.BgDeep

/**
 * Champs "métier" (SSID, mot de passe wifi, comptes admin) visibles
 * directement — c'est ce que le patron du cyber-café touche vraiment.
 * Tout ce qui est réseau/CIDR/DHCP est technique et va dans SectionAvancee,
 * repliée par défaut, avec les valeurs par défaut déjà pré-remplies.
 */
@Preview
@Composable
fun TemplateContent(
    templateParams: CyberTemplateParams = CyberTemplateParams(),
    onBack: () -> Unit = {},
    onContinue: (CyberTemplateParams) -> Unit = {},
) {

    var ssid24 by remember { mutableStateOf(templateParams.ssid24) }
    var ssid5 by remember { mutableStateOf(templateParams.ssid5) }
    var wifiPassword by remember { mutableStateOf(templateParams.wifiPassword) }
    var adminCount by remember { mutableStateOf(templateParams.adminCount.toString()) }

    var lanCidr by remember { mutableStateOf(templateParams.lanCidr) }
    var routerIp by remember { mutableStateOf(templateParams.routerIp) }
    var dhcpStart by remember { mutableStateOf(templateParams.dhcpPoolStart) }
    var dhcpEnd by remember { mutableStateOf(templateParams.dhcpPoolEnd) }

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            TitreWizard(
                title = "Configuration réseau",
                subtitle = "Nom et mot de passe du Wi-Fi du cyber-café",
            )
        },
        bottomBar = {
            BarreActionsWizard(
                onBack = onBack,
                onContinue = {
                    val updatedParans = templateParams.copy(
                        ssid24 = ssid24,
                        ssid5 = ssid5,
                        wifiPassword = wifiPassword,
                        adminCount = adminCount.toInt(),
                        lanCidr = lanCidr,
                        routerIp = routerIp,
                        dhcpPoolStart = dhcpStart,
                        dhcpPoolEnd = dhcpEnd,
                    )
                    onContinue(updatedParans)
                },
                continueEnabled = ssid24.isNotBlank() && wifiPassword.length >= 8,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
            ,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            SurfaceCard {
                ChampWizard(label = "SSID 2.4 GHz", value = ssid24, onValueChange = { ssid24 = it }, maxlength = 32, isSsid = true)
                ChampWizard(label = "SSID 5 GHz", value = ssid5, onValueChange = { ssid5 = it }, maxlength = 32, isSsid = true)
                ChampWizard(label = "Mot de passe Wi-Fi", value = wifiPassword, onValueChange = { wifiPassword = it }, isPassword = true)
                ChampWizard(label = "Nombre de comptes admin", value = adminCount, onValueChange = { adminCount = it }, isNumeric = true, minValue = 1, maxValue = 5)
            }

            SectionAvancee {
                ChampWizard(label = "Sous-réseau (CIDR)", value = lanCidr, onValueChange = { lanCidr = it })
                ChampWizard(label = "IP du routeur", value = routerIp, onValueChange = { routerIp = it })
                ChampWizard(label = "Début pool DHCP", value = dhcpStart, onValueChange = { dhcpStart = it })
                ChampWizard(label = "Fin pool DHCP", value = dhcpEnd, onValueChange = { dhcpEnd = it })
            }

        }
    }
}