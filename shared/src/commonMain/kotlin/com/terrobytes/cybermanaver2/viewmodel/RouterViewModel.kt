package com.terrobytes.cybermanaver2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrobytes.cybermanaver2.models.RouterUiState
import com.terrobytes.cybermanaver2.models.Routeur
import com.terrobytes.cybermanaver2.network.InfoNetworkDevice
import com.terrobytes.cybermanaver2.network.MikrotikRawClient
import com.terrobytes.cybermanaver2.network.WifiRouterScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RouterViewModel : ViewModel() {

    private val scanner: WifiRouterScanner = WifiRouterScanner()
    private val _state = MutableStateFlow(RouterUiState())
    val state = _state.asStateFlow()

    val listeBaseIp = InfoNetworkDevice.getNetworkBaseIp()

    fun startScan() {
        viewModelScope.launch {

            if (_state.value.isScanning) return@launch

            _state.value = _state.value.copy(
                isScanning = true,
                progress = 0,
                routers = emptyList()
            )

            for (baseIp in listeBaseIp) {
                _state.value = _state.value.copy(baseIp = baseIp)

                val results = scanner.scanWifi(baseIp) { progress ->
                    _state.value = _state.value.copy(progress = progress)
                }

                _state.value = _state.value.copy(
                    isScanning = false,
                    routers = results,
                    progress = 254
                )

                if (results.isNotEmpty()) break
            }


        }
    }

    fun selectRouter(router: Routeur) {
        if (state.value.selectedRouter != router) {
            _state.value = _state.value.copy(
                selectedRouter = router
            )
        } else {
            _state.value = _state.value.copy(
                selectedRouter = null,
            )
        }
    }

    fun connect() {
        testConnection(state.value.selectedRouter!!.ipAddress)
    }

    fun connection(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val client = MikrotikRawClient(ip)
            val ok = client.testPort()
            if (!ok) return@launch

        }
    }

    fun testConnection(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {

            val client = MikrotikRawClient(ip)

            val ok = client.testPort()

            if (!ok) return@launch

            val loginOk = client.login("admin", "brayan1234567")

            if (!loginOk) return@launch

            val result = client.execute("/system/resource/print")

            println(result)

            _state.value = _state.value.copy(
                result = result
            )

            client.close()
        }
    }
}