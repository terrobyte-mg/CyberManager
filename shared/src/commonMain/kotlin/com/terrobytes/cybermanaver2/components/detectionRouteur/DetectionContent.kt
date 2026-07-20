package com.terrobytes.cybermanaver2.components.detectionRouteur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.terrobytes.cybermanaver2.ui.composable.drs.BouttonAction
import com.terrobytes.cybermanaver2.ui.composable.drs.CarteAnalyseur
import com.terrobytes.cybermanaver2.ui.composable.drs.CarteRouteur
import com.terrobytes.cybermanaver2.ui.composable.drs.TitreDetecteur

@Composable
fun DetectionContent(
    component: DetectionComponent
) {

    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TitreDetecteur(
                modifier = Modifier
                    .background(Color(0xFF0B1319))
                    .statusBarsPadding()
            )
        },
        bottomBar = {
            BouttonAction(
                onConnectClick = { component.connect() },
                onRefreshClick = { component.startScan() },
                onManualConnectClick = { component.connectManually() },
                isConnecting = state.isScanning,
                hasSelection = state.selectedRouter != null,
                refreshEnabled = !state.isScanning,
                modifier = Modifier.navigationBarsPadding().padding(16.dp)
            )
        }
    ) { innerPadding ->


        Column(
            Modifier
                .background(Color(0xFF0B1319))
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            CarteAnalyseur(
                isScanning = state.isScanning,
                progress = state.progress,
                total = state.total,
                baseIp = state.baseIp,
            )
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(state.routers) { router ->
                    CarteRouteur(
                        routeur = router,
                        isSelected = router == state.selectedRouter,
                        onSelectClick = {
                            component.selectRouter(router)
                        }
                    )
                }
            }
        }

    }

}