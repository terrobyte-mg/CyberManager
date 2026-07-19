package com.terrobytes.cybermanaver2.ui.composable.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.terrobytes.cybermanaver2.ui.composable.colors.BgDeep

/**
 * Gabarit commun aux 3 étapes longues. [logsSimules] et le passage
 * EnAttente -> Terminé au clic sur "Démarrer" sont factices, juste pour
 * visualiser l'écran - à remplacer par le vrai run du composant plus tard.
 */
@Composable
private fun StepRunContent(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    logsSimules: List<String>,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var statut by remember { mutableStateOf<EtapeStatut>(EtapeStatut.EnAttente) }

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            TitreWizard(title = title, subtitle = subtitle)
        },
        bottomBar = {
            BarreActionsWizard(
                onBack = onBack,
                onContinue = onContinue,
                continueLabel = "Continuer",
                continueEnabled = statut == EtapeStatut.Terminee,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CarteEtapeLongue(
                icon = icon,
                statut = statut,
                logs = if (statut == EtapeStatut.EnAttente) emptyList() else logsSimules,
                onDemarrer = { statut = EtapeStatut.EnCours; statut = EtapeStatut.Terminee },
            )
        }
    }
}

@Composable
fun BackupContent(onBack: () -> Unit, onContinue: () -> Unit) {
    StepRunContent(
        title = "Sauvegarde de la configuration",
        subtitle = "Filet de sécurité avant le reset",
        icon = Icons.Filled.CloudUpload,
        logsSimules = listOf("[backup] connexion au routeur…", "[backup] export config…", "[backup] terminé"),
        onBack = onBack,
        onContinue = onContinue,
    )
}

@Composable
fun InjectionContent(onBack: () -> Unit, onContinue: () -> Unit) {
    StepRunContent(
        title = "Injection + reset",
        subtitle = "Le routeur va redémarrer",
        icon = Icons.Filled.Router,
        logsSimules = listOf("[inject] upload script…", "[inject] déclenchement reset…", "[inject] terminé"),
        onBack = onBack,
        onContinue = onContinue,
    )
}

@Composable
fun VerifyContent(onBack: () -> Unit, onContinue: () -> Unit) {
    StepRunContent(
        title = "Reconnexion et vérification",
        subtitle = "Annulation du filet de sécurité",
        icon = Icons.Filled.Wifi,
        logsSimules = listOf("[verify] reconnexion wifi…", "[verify] marqueur détecté", "[verify] failsafe annulé"),
        onBack = onBack,
        onContinue = onContinue,
    )
}