package com.terrobytes.cybermanaver2.components.login

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.terrobytes.cybermanaver2.components.utils.InstanceHolder
import com.terrobytes.cybermanaver2.models.RouterDiscoveryMethod
import com.terrobytes.cybermanaver2.models.RouterUiState
import com.terrobytes.cybermanaver2.models.Routeur
import com.terrobytes.cybermanaver2.network.MikrotikRawClient
import com.terrobytes.cybermanaver2.network.NetworkTarget
import com.terrobytes.cybermanaver2.storage.CredentialsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultLoginComponent(
    componentContext: ComponentContext,
    ipAddress : String,
    private val name : String,
    private val networkTarget: NetworkTarget?,
    private val onBackClicked : () -> Unit,
) : LoginComponent, ComponentContext by componentContext {

    private val instanceHolder = instanceKeeper.getOrCreate { InstanceHolder() }
    private val scope get() = instanceHolder.scope

    private val credentialsStore : CredentialsStore = CredentialsStore()

    private val _routeurState = MutableValue(
        RouterUiState(
            selectedRouter = Routeur(
                ipAddress = ipAddress,
                name = name,
                source = RouterDiscoveryMethod.SCAN_WIFI,
                networkTarget = networkTarget,
            )
        )
    )
    override val routeurState: Value<RouterUiState> = _routeurState

    override fun submitLogin(username: String, password: String) {

        val routeur = routeurState.value.selectedRouter ?: return

        if (username.isBlank() || password.isBlank()) {
            _routeurState.value = _routeurState.value.copy(loginError = "Veuillez renseigner l'identifiant et le mot de passe")
            return
        }

        scope.launch(Dispatchers.IO) {
            _routeurState.value = _routeurState.value.copy(isLoggingIn = true, loginError = null)

            try {
                val client = MikrotikRawClient(networkTarget, routeur.ipAddress)
                val loginOk = client.login(username, password)
                if (!loginOk) {
                    client.close()
                    _routeurState.value = _routeurState.value.copy(
                        isLoggingIn = false,
                        loginError = "Identifiant ou mot de passe incorrect"
                    )
                    return@launch
                }

                credentialsStore.saveCredentials(username, password)

                val result = client.execute("/system/resource/print")
                client.close()

                _routeurState.value = _routeurState.value.copy(
                    isLoggingIn = false,
                    isAuthenticated = true,
                    result = result
                )

            } catch (e : Exception) {
                _routeurState.value = _routeurState.value.copy(
                    isLoggingIn = false,
                    isAuthenticated = false,
                    loginError = e.message ?: e.toString()
                )
            }


        }

    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }


}