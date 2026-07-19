package com.terrobytes.cybermanaver2.components.manuallyConnection

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.terrobytes.cybermanaver2.components.utils.InstanceHolder
import com.terrobytes.cybermanaver2.network.MikrotikRawClient
import com.terrobytes.cybermanaver2.network.MikrotikSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultManuallyConnexionComponent(
    componentContext: ComponentContext,
    private val onGoBackClicked: () -> Unit,
    sessionManager: MikrotikSessionManager,
) : ManuallyConnexionComponent, ComponentContext by componentContext {

    private val instanceHolder = instanceKeeper.getOrCreate { InstanceHolder() }
    private val scope get() = instanceHolder.scope

    override fun onConnectClicked(ipAddress: String, username: String, password: String) {

        scope.launch(Dispatchers.IO) {

            try {
                val client = MikrotikRawClient(networkTarget = null, host = ipAddress)

                val loginOk = client.login(username, password)
                if (!loginOk) {
                    client.close()
                    return@launch
                }

                println(client.execute("/system/resource/print"))
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }


        }

    }

    override fun onGoBackClicked() {
        onGoBackClicked.invoke()
    }

}