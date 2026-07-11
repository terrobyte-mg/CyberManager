package com.terrobytes.cybermanaver2.components.detectionRouteur

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.terrobytes.cybermanaver2.components.utils.InstanceHolder
import com.terrobytes.cybermanaver2.models.RouterUiState
import com.terrobytes.cybermanaver2.models.Routeur
import com.terrobytes.cybermanaver2.network.InfoNetworkDevice
import com.terrobytes.cybermanaver2.network.MikrotikRawClient
import com.terrobytes.cybermanaver2.network.NetworkTarget
import com.terrobytes.cybermanaver2.network.WifiRouterScanner
import com.terrobytes.cybermanaver2.storage.CredentialsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultDetectionComponent(
    componentContext: ComponentContext,
    private val onConnectClicked: (router : Routeur) -> Unit,
    private val onConnectManuallyClicked: () -> Unit,
) : DetectionComponent, ComponentContext by componentContext {

    private val instanceHolder = instanceKeeper.getOrCreate { InstanceHolder() }
    private val scope get() = instanceHolder.scope

    private val credentialsStore : CredentialsStore = CredentialsStore()

    private val _state = MutableValue(RouterUiState())
    override val state : Value<RouterUiState> = _state

    private val _listeNetworks = MutableValue(InfoNetworkDevice.getNetworks())
    val listeNetworks : Value<List<NetworkTarget>> = _listeNetworks

    init {
        _state.value = _state.value.copy(
            isAuthenticated = credentialsStore.getCredentials() != null
        )
    }

    private fun resyncListeBaseIp() {
        _listeNetworks.value = InfoNetworkDevice.getNetworks()
        _state.value = _state.value.copy(result = _listeNetworks.value.toString())
    }

    override fun startScan() {
        if (_state.value.isScanning) return

        resyncListeBaseIp()

        scope.launch {

            _state.value = _state.value.copy(
                isScanning = true,
                progress = 0,
                routers = emptyList()
            )

            val routers = mutableListOf<Routeur>()

            for (network in listeNetworks.value) {
                _state.value = _state.value.copy(baseIp = network.baseIp)

                routers.addAll(WifiRouterScanner.scanWifi(network) { progress ->
                    _state.value = _state.value.copy(progress = progress)
                })

                _state.value = _state.value.copy(
                    isScanning = false,
                    routers = routers,
                    progress = 254
                )

                if (_state.value.routers.isNotEmpty()) break
            }

        }
    }

    override fun selectRouter(routeur: Routeur) {
        if (_state.value.selectedRouter != routeur) {
            _state.value = _state.value.copy(
                selectedRouter = routeur
            )
        } else {
            _state.value = _state.value.copy(
                selectedRouter = null
            )
        }
    }

    override fun connect() {
        val routeur = state.value.selectedRouter ?: return
        val saved = credentialsStore.getCredentials()

        if (saved != null) {
            testConnection(routeur.ipAddress, saved.first, saved.second)
        } else {
            onConnectClicked(routeur)
        }
    }

    override fun connectManually() {
        onConnectManuallyClicked()
    }

    override fun testConnection(ip: String, username: String, password: String) {
        scope.launch(Dispatchers.IO) {

            val networkTarget = state.value.selectedRouter?.networkTarget
            val client = MikrotikRawClient(networkTarget, ip)

            try {

                val loginOk = client.login(username, password)
                if (!loginOk) {
                    credentialsStore.clearCredentials()
                    _state.value = _state.value.copy(
                        isAuthenticated = false,
                        loginError = "Session expiree, veuillez vous reconnecter"
                    )
                    return@launch
                }

                val result = client.execute("/system/resource/print")
                _state.value = _state.value.copy(result = result, isAuthenticated = true)

            } catch (e: Exception) {
                _state.value = _state.value.copy(loginError = "Erreur de connexion: ${e.message}")
            } finally {
                client.close()
            }
        }
    }



}