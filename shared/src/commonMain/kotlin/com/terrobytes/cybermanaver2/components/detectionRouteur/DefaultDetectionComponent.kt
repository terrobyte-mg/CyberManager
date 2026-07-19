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
import com.terrobytes.cybermanaver2.network.MikrotikSessionManager
import com.terrobytes.cybermanaver2.network.NetworkTarget
import com.terrobytes.cybermanaver2.network.WifiRouterScanner
import com.terrobytes.cybermanaver2.network.parseApiValue
import com.terrobytes.cybermanaver2.storage.CredentialsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultDetectionComponent(
    componentContext: ComponentContext,
    private val onConnectClicked: (router: Routeur) -> Unit,
    private val onConnectManuallyClicked: () -> Unit,
    private val onFirstParameter: (router: Routeur) -> Unit,
    private val sessionManager: MikrotikSessionManager,
) : DetectionComponent, ComponentContext by componentContext {

    private val instanceHolder = instanceKeeper.getOrCreate { InstanceHolder() }
    private val scope get() = instanceHolder.scope

    private val credentialsStore: CredentialsStore = CredentialsStore()

    private val _state = MutableValue(RouterUiState())
    override val state: Value<RouterUiState> = _state

    private val _listeNetworks = MutableValue(InfoNetworkDevice.getNetworks())
    val listeNetworks: Value<List<NetworkTarget>> = _listeNetworks

    init {
        _state.value = _state.value.copy(
            isAuthenticated = credentialsStore.getCredentials() != null
        )
        startScan()
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

                if (_state.value.routers.isNotEmpty()) {
                    selectRouter(_state.value.routers[0])
                    connect()
                    break
                }
            }
        }
    }

    override fun selectRouter(routeur: Routeur) {
        _state.value = _state.value.copy(
            selectedRouter = if (_state.value.selectedRouter != routeur) routeur else null
        )
    }

    /**
     * Point d'entrée UI. Attend le vrai résultat du login avant de décider
     * quoi faire ensuite — plus de course entre login async et vérification.
     */
    override fun connect() {
        val routeur = state.value.selectedRouter ?: return
        val saved = credentialsStore.getCredentials()

        if (saved == null) {
            onConnectClicked(routeur)
            return
        }

        scope.launch(Dispatchers.Main) {
            val loginOk = withContext(Dispatchers.IO) {
                doLogin(routeur, saved.first, saved.second)
            }

            if (loginOk) {
                withContext(Dispatchers.IO) {
                    doReadWifiSsids()
                    doVerifyConfigRouter()
                }
            } else {
                onConnectClicked(routeur)
            }
        }
    }

    override fun connectManually() {
        onConnectManuallyClicked()
    }

    /**
     * Réalise le login réseau et met à jour la session partagée si ça
     * réussit. Toujours appelée depuis Dispatchers.IO.
     */
    private suspend fun doLogin(routeur: Routeur, username: String, password: String): Boolean {
        val networkTarget = routeur.networkTarget

        val client = try {
            sessionManager.client.value ?: MikrotikRawClient(networkTarget, routeur.ipAddress)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(loginError = "Connexion impossible: ${e.message}")
            }
            return false
        }

        return try {
            val loginOk = client.login(username, password)
            if (!loginOk) {
                client.close()
                credentialsStore.clearCredentials()
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        isAuthenticated = false,
                        loginError = "Session expirée, veuillez vous reconnecter"
                    )
                }
                false
            } else {
                sessionManager.setClient(client)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isAuthenticated = true, loginError = null)
                }
                true
            }
        } catch (e: Exception) {
            client.close()
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(loginError = "Erreur de connexion: ${e.message}")
            }
            false
        }
    }

    /**
     * Vérifie si la config vient de nous (marqueur CyberManager) une fois
     * qu'on est déjà loggué. Appelée seulement après un login réussi, donc
     * sessionManager.client.value est garanti non-null et fraîchement
     * authentifié à ce stade.
     */
    private suspend fun doVerifyConfigRouter() {
        val client = sessionManager.client.value ?: return

        val note = try {
            client.execute("/system/note/print")
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(loginError = "Erreur de vérification: ${e.message}")
            }
            return
        }

        val noteValue = parseApiValue(note, "note") ?: ""

        if (noteValue.contains("CyberManager")) {
            // TODO: une fois RouterInjectionManager mis à jour pour persister
            // les creds du compte marqueur (MARKER_API_USERNAME) séparément
            // dans CredentialsStore, c'est ici qu'il faut relire ces
            // creds dédiées et basculer sessionManager.setClient(...) dessus
            // - au lieu de rouvrir une connexion avec les mêmes creds admin
            // qu'on vient déjà d'utiliser, ce qui n'apporte rien.
        } else {
            scope.launch(Dispatchers.Main) { onFirstParameter(_state.value.selectedRouter ?: return@launch) }
        }
    }

    // --- Méthodes de l'interface, utilisables isolément depuis l'UI (fire-and-forget) ---

    override fun testConnection(ip: String, username: String, password: String): Boolean {
        val routeur = state.value.selectedRouter ?: return false
        scope.launch(Dispatchers.IO) { doLogin(routeur, username, password) }
        return true
    }

    override fun verifyConfigRouter(): Boolean {
        scope.launch(Dispatchers.IO) { doVerifyConfigRouter() }
        return true
    }

    /**
     * Lit les interfaces wifis du routeur et en extrait le SSID 2.4GHz et,
     * s'il existe, le SSID 5GHz. Doit être appelée avec un client déjà loggué.
     *
     * La réponse API contient un bloc " ! Re" par interface wifi trouvée, donc on
     * découpe la réponse brute par bloc avant de lire "band" et "ssid" dans
     * chacun — un simple parseApiValue (raw, key) ne suffit pas ici puisqu'il ne
     * renverrait que la première occurrence toutes interfaces confondues.
     */
    private fun readWifiSsids(client: MikrotikRawClient): Pair<String?, String?> {
        val raw = client.execute("/interface/wireless/print")

        var ssid24: String? = null
        var ssid5: String? = null

        val blocks = raw.split("!re\n").drop(1)

        for (block in blocks) {
            val band = parseApiValue("!re\n$block", "band") ?: continue
            val ssid = parseApiValue("!re\n$block", "ssid") ?: continue

            when {
                band.contains("2ghz") -> if (ssid24 == null) ssid24 = ssid
                band.contains("5ghz") -> if (ssid5 == null) ssid5 = ssid
            }
        }

        return ssid24 to ssid5
    }

    private suspend fun doReadWifiSsids() {
        val client = sessionManager.client.value ?: return
        val currrent = _state.value.selectedRouter ?: return

        val (ssid24, ssid5) = try {
            readWifiSsids(client)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(commandError = "Erreur de lecture wifi: ${e.message}")
            }
            return
        }

        withContext(Dispatchers.Main) {
            val updated = currrent.copy(ssid24 = ssid24, ssid5 = ssid5)
            _state.value = _state.value.copy(
                selectedRouter = updated,
                routers = _state.value.routers.map { if (it.ipAddress == updated.ipAddress) updated else it }
            )
        }
    }

}