package com.terrobytes.cybermanaver2.components.parametrageReseaux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.terrobytes.cybermanaver2.templates.CyberTemplateParams
import com.terrobytes.cybermanaver2.ui.composable.wizard.TemplateContent

@Composable
fun ParametrageReseauxContent(component: ParametrageReseauxComponent) {

    val templateState by component.templateState.subscribeAsState()

    TemplateContent(
        templateState,
        onBack = { component.onBack() },
        onContinue = { updatedParams ->
            component.onContinue(updatedParams)
        },
    )

}