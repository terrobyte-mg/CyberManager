package com.terrobytes.cybermanaver2.components.parametrageReseaux

import com.arkivanov.decompose.value.Value
import com.terrobytes.cybermanaver2.templates.CyberTemplateParams

interface ParametrageReseauxComponent {
    val templateState : Value<CyberTemplateParams>
    fun onBack()
    fun onContinue(template : CyberTemplateParams)
}