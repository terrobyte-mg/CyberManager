package com.terrobytes.cybermanaver2.components.parametrageReseaux

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.terrobytes.cybermanaver2.network.MikrotikSessionManager
import com.terrobytes.cybermanaver2.templates.CyberTemplateParams

class DefaultParametrageReseauxComponent(
    ssid24: String,
    ssid5: String,
    componentContext: ComponentContext,
    sessionManager: MikrotikSessionManager,
) : ParametrageReseauxComponent, ComponentContext by componentContext {

    private val _templateState = MutableValue(
        CyberTemplateParams(
            ssid24 = ssid24,
            ssid5 = ssid5
        )
    )

    override val templateState: Value<CyberTemplateParams> = _templateState

    override fun onContinue(template: CyberTemplateParams) {
        TODO("Not yet implemented")
    }

    override fun onBack() {
        TODO("Not yet implemented")
    }
}