package com.terrobytes.cybermanaver2.ui.composable.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.terrobytes.cybermanaver2.ui.composable.colors.BgDeep

/**
 * Fichier regroupant toutes les @Preview du wizard — un seul endroit à
 * ouvrir dans le panneau Preview d'Android Studio pour tout voir d'un coup.
 * Rien ici n'est branché à un vrai composant : état 100% local.
 */

// ─── Primitives drs, isolées ───────────────────────────────────────────

@Preview(name = "drs - TitreWizard", showBackground = true, backgroundColor = 0xFF0B1319)
@Composable
private fun PreviewTitreWizard() {
    TitreWizard(
        title = "Configuration réseau",
        subtitle = "Nom et mot de passe du Wi-Fi du cyber-café"
    )
}

//@Preview(name = "drs - ChampWizard (normal)", showBackground = true, backgroundColor = 0xFF0B1319)
//@Composable
//private fun PreviewChampWizard() {
//    var value by remember { mutableStateOf("CyberManager-Test") }
//    Column(Modifier.background(BgDeep).padding(16.dp)) {
//        ChampWizard(label = "SSID 2.4 GHz", value =  value)
//    }
//}

//@Preview(name = "drs - ChampWizard (erreur)", showBackground = true, backgroundColor = 0xFF0B1319)
//@Composable
//private fun PreviewChampWizardErreur() {
//    var value by remember { mutableStateOf("abc") }
//    Column(Modifier.background(BgDeep).padding(16.dp)) {
//        ChampWizard(label = "Mot de passe Wi-Fi", value =  value, isPassword = true, errorMessage = "8 caractères minimum")
//    }
//}

//@Preview(name = "drs - SectionAvancee", showBackground = true, backgroundColor = 0xFF0B1319)
//@Composable
//private fun PreviewSectionAvancee() {
//    var cidr by remember { mutableStateOf("192.168.88.0/24") }
//    Column(Modifier.background(BgDeep).padding(16.dp)) {
//        SectionAvancee {
//            ChampWizard(label = "Sous-réseau (CIDR)", value =  cidr)
//        }
//    }
//}

@Preview(name = "drs - BarreActionsWizard (complète)", showBackground = true, backgroundColor = 0xFF141C24)
@Composable
private fun PreviewBarreActionsComplete() {
    BarreActionsWizard(onBack = {}, onContinue = {})
}

@Preview(name = "drs - BarreActionsWizard (1ère étape)", showBackground = true, backgroundColor = 0xFF141C24)
@Composable
private fun PreviewBarreActionsPremiereEtape() {
    BarreActionsWizard(onBack = null, onContinue = {})
}

@Preview(name = "drs - CarteEtapeLongue (en attente)", showBackground = true, backgroundColor = 0xFF0B1319)
@Composable
private fun PreviewCarteEtapeAttente() {
    Column(Modifier.background(BgDeep).padding(16.dp)) {
        CarteEtapeLongue(
            icon = Icons.Filled.CloudUpload,
            statut = EtapeStatut.EnAttente,
            logs = emptyList(),
            onDemarrer = {},
        )
    }
}

@Preview(name = "drs - CarteEtapeLongue (en cours)", showBackground = true, backgroundColor = 0xFF0B1319)
@Composable
private fun PreviewCarteEtapeEnCours() {
    Column(Modifier.background(BgDeep).padding(16.dp)) {
        CarteEtapeLongue(
            icon = Icons.Filled.CloudUpload,
            statut = EtapeStatut.EnCours,
            logs = listOf("[backup] connexion au routeur…"),
            onDemarrer = {},
        )
    }
}

@Preview(name = "drs - CarteEtapeLongue (terminée)", showBackground = true, backgroundColor = 0xFF0B1319)
@Composable
private fun PreviewCarteEtapeTerminee() {
    Column(Modifier.background(BgDeep).padding(16.dp)) {
        CarteEtapeLongue(
            icon = Icons.Filled.CloudUpload,
            statut = EtapeStatut.Terminee,
            logs = listOf("[backup] connexion au routeur…", "[backup] export config…", "[backup] terminé"),
            onDemarrer = {},
        )
    }
}

@Preview(name = "drs - CarteEtapeLongue (échec)", showBackground = true, backgroundColor = 0xFF0B1319)
@Composable
private fun PreviewCarteEtapeEchec() {
    Column(Modifier.background(BgDeep).padding(16.dp)) {
        CarteEtapeLongue(
            icon = Icons.Filled.CloudUpload,
            statut = EtapeStatut.Echouee("Connexion au routeur refusée"),
            logs = listOf("[backup] connexion au routeur…"),
            onDemarrer = {},
        )
    }
}

// ─── Écrans complets ────────────────────────────────────────────────────

@Preview(name = "Écran 2 - Template", showBackground = true, backgroundColor = 0xFF0B1319, device = "id:pixel_6")
@Composable
private fun PreviewTemplateContent() {
    TemplateContent(onBack = {}, onContinue = {})
}

@Preview(name = "Écran 3 - Backup", showBackground = true, backgroundColor = 0xFF0B1319, device = "id:pixel_6")
@Composable
private fun PreviewBackupContent() {
    BackupContent(onBack = {}, onContinue = {})
}

@Preview(name = "Écran 4 - Injection", showBackground = true, backgroundColor = 0xFF0B1319, device = "id:pixel_6")
@Composable
private fun PreviewInjectionContent() {
    InjectionContent(onBack = {}, onContinue = {})
}

@Preview(name = "Écran 5 - Vérification", showBackground = true, backgroundColor = 0xFF0B1319, device = "id:pixel_6")
@Composable
private fun PreviewVerifyContent() {
    VerifyContent(onBack = {}, onContinue = {})
}

//@Preview(name = "Écran 6 - Terminé", showBackground = true, backgroundColor = 0xFF0B1319, device = "id:pixel_6")
//@Composable
//private fun PreviewDoneContent() {
//    DoneContent()
//}

//// ─── Parcours interactif complet ────────────────────────────────────────
//
//@Preview(name = "Parcours complet (interactif)", showBackground = true, backgroundColor = 0xFF0B1319, device = "id:pixel_6", heightDp = 800)
//@Composable
//private fun PreviewWizardHost() {
//    WizardHost()
//}