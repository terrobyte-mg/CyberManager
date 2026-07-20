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
    private val onBackClicked: () -> Unit,
    private val onContinueClicked: (CyberTemplateParams) -> Unit,
) : ParametrageReseauxComponent, ComponentContext by componentContext {

    private val _templateState = MutableValue(
        CyberTemplateParams(ssid24 = ssid24, ssid5 = ssid5)
    )

    override val templateState: Value<CyberTemplateParams> = _templateState

    override fun onContinue(template: CyberTemplateParams) {
        _templateState.value = template
        onContinueClicked(template)
    }

    override fun onBack() {
        onBackClicked()
    }
}