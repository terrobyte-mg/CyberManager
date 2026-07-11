package com.terrobytes.cybermanaver2.components.manuallyConnection

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.terrobytes.cybermanaver2.ui.composable.drs.CardManuallyConnexion

@Composable
fun ManuallyConnexionContent(
    component: ManuallyConnexionComponent
) {

    Column {
        CardManuallyConnexion(
            onCancel = { component.onGoBackClicked() },
            onSubmit = { ipAddress, username, password ->
                component.onConnectClicked(ipAddress, username, password)
            }
        )
    }

}