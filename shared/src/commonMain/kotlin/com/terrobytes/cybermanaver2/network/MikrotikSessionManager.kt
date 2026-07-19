package com.terrobytes.cybermanaver2.network

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MikrotikSessionManager : InstanceKeeper.Instance {

    private val _client = MutableStateFlow<MikrotikRawClient?>(null)
    val client: StateFlow<MikrotikRawClient?> = _client

    fun setClient(client: MikrotikRawClient?) {
        _client.value?.close()
        _client.value = client
    }

    fun disconnect() {
        _client.value?.close()
        _client.value = null
    }

    override fun onDestroy() {
        disconnect()
    }

}