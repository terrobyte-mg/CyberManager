//package com.terrobytes.cybermanaver2.ui.composable.wizard
//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.terrobytes.cybermanaver2.ui.composable.colors.BgDeep
//
///**
// * Version maquette : état 100% local, [onContinue] est juste un signal de
// * navigation vers l'étape suivante - pas d'appel réseau ici.
// */
//@Composable
//fun ConnectionContent(
//    onContinue: () -> Unit,
//) {
//    var host by remember { mutableStateOf("192.168.88.1") }
//    var username by remember { mutableStateOf("admin") }
//    var password by remember { mutableStateOf("") }
//
//    Scaffold(
//        containerColor = BgDeep,
//        topBar = {
//            TitreWizard(
//                title = "Connexion au routeur",
//                subtitle = "Identifiants admin de l'interface RouterOS",
//                stepIndex = 0,
//                stepCount = 5,
//            )
//        },
//        bottomBar = {
//            BarreActionsWizard(
//                onBack = null,
//                onSkip = null,
//                onContinue = onContinue,
//                continueEnabled = host.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
//            )
//        },
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp),
//        ) {
//            SurfaceCard {
//                ChampWizard("Adresse IP", host, { host = it })
//                ChampWizard("Utilisateur", username, { username = it })
//                ChampWizard("Mot de passe", password, { password = it }, isPassword = true)
//            }
//        }
//    }
//}