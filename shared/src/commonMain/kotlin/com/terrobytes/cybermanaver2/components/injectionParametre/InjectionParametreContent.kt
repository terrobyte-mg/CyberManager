package com.terrobytes.cybermanaver2.components.injectionParametre

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.terrobytes.cybermanaver2.network.InjectionStep
import com.terrobytes.cybermanaver2.ui.composable.wizard.CarteEtapeLongue
import com.terrobytes.cybermanaver2.ui.composable.wizard.EtapeStatut

@Composable
fun InjectionParametreContent(component: InjectionParametreComponent) {

    val state by component.uiState.subscribeAsState()

    val statut = when (state.phase) {
        InjectionStep.GaveUpWaiting -> EtapeStatut.Echouee(
            "Reconnexion impossible - le routeur va restaurer l'ancienne configuration automatiquement"
        )
        InjectionStep.Done -> EtapeStatut.Terminee
        else -> EtapeStatut.EnCours
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        CarteEtapeLongue(
            icon = Icons.Filled.Router,
            statut = statut,
            logs = state.logs,
            onDemarrer = { /* déjà lancé automatiquement */ },
            onReessayer = { component.onRetry() },
        )
    }
}