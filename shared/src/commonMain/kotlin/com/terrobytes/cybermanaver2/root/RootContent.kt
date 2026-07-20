package com.terrobytes.cybermanaver2.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.terrobytes.cybermanaver2.components.dashboard.DashboardContent
import com.terrobytes.cybermanaver2.components.detectionRouteur.DetectionContent
import com.terrobytes.cybermanaver2.components.injectionParametre.InjectionParametreContent
import com.terrobytes.cybermanaver2.components.login.LoginContent
import com.terrobytes.cybermanaver2.components.manuallyConnection.ManuallyConnexionContent
import com.terrobytes.cybermanaver2.components.parametrageReseaux.ParametrageReseauxContent

@Composable
fun RootContent(
    modifier : Modifier = Modifier,
    component : RootComponent
) {

    Children(
        stack = component.childStack,
        modifier = modifier,
        animation = stackAnimation(slide()),
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Detection -> DetectionContent(instance.component)
            is RootComponent.Child.Login -> LoginContent(instance.component)
            is RootComponent.Child.ManuallyConnexion -> ManuallyConnexionContent(instance.component)
            is RootComponent.Child.ParametrageReseaux -> ParametrageReseauxContent(instance.component)
            is RootComponent.Child.InjectionParametre -> InjectionParametreContent(instance.component)
            is RootComponent.Child.Dashboard -> DashboardContent(instance.component)
        }
    }

}