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
import com.terrobytes.cybermanaver2.network.MikrotikSessionManager
import com.terrobytes.cybermanaver2.network.NetworkTarget
import com.terrobytes.cybermanaver2.storage.CredentialsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultLoginComponent(
    componentContext: ComponentContext,
    ipAddress: String,
    private val name: String,
    private val networkTarget: NetworkTarget?,
    private val onBackClicked: () -> Unit,
    private val onSubmitClicked: (routeur: Routeur) -> Unit,
    private val sessionManager: MikrotikSessionManager,
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

    override fun submitLogin(username: String, password: String, test: Boolean) {
        val routeur = routeurState.value.selectedRouter ?: return

        if (username.isBlank()) {
            _routeurState.value = _routeurState.value.copy(loginError = "Veuillez renseigner l'identifiant")
            return
        }

        scope.launch(Dispatchers.Main) {
            _routeurState.value = _routeurState.value.copy(isLoggingIn = true, loginError = null)

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val client = MikrotikRawClient(networkTarget, routeur.ipAddress)
                    val loginOk = client.login(username, password)
                    if (!loginOk) {
                        client.close()
                        null
                    } else {
                        client
                    }
                }
            }

            result.fold(
                onSuccess = { client ->
                    if (client == null) {
                        _routeurState.value = _routeurState.value.copy(
                            isLoggingIn = false,
                            loginError = if (test) null else "Identifiant ou mot de passe incorrect"
                        )
                        return@fold
                    }

                    credentialsStore.saveCredentials(username, password)
                    sessionManager.setClient(client)

                    _routeurState.value = _routeurState.value.copy(
                        isLoggingIn = false,
                        isAuthenticated = true,
                    )

                    onSubmitClicked(routeur)
                },
                onFailure = { e ->
                    _routeurState.value = _routeurState.value.copy(
                        isLoggingIn = false,
                        isAuthenticated = false,
                        loginError = e.message ?: e.toString()
                    )
                }
            )
        }
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }
}