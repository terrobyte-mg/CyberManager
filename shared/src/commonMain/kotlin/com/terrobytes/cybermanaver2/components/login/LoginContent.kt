package com.terrobytes.cybermanaver2.components.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.terrobytes.cybermanaver2.ui.composable.drs.CardAuthentification

@Composable
fun LoginContent(
    component : LoginComponent,
    modifier: Modifier = Modifier,
    ) {

    LaunchedEffect(component) {
        component.submitLogin("admin", "", true)
    }

    val routeurState by component.routeurState.subscribeAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(0xFF141C24)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        CardAuthentification(
            routerName = routeurState.selectedRouter!!.name,
            isLoading = routeurState.isLoggingIn,
            errorMessage = routeurState.loginError,
            onSubmit = { username, password -> component.submitLogin(username, password) },
            onCancel = component::onBackClicked,
            modifier = Modifier.padding(16.dp)
        )

        Text(routeurState.result)

    }

}