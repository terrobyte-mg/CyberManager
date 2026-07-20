package com.terrobytes.cybermanaver2.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.terrobytes.cybermanaver2.components.dashboard.DashboardComponent
import com.terrobytes.cybermanaver2.components.dashboard.DefaultDashboardComponent
import com.terrobytes.cybermanaver2.components.detectionRouteur.DefaultDetectionComponent
import com.terrobytes.cybermanaver2.components.detectionRouteur.DetectionComponent
import com.terrobytes.cybermanaver2.components.injectionParametre.DefaultInjectionParametreComponent
import com.terrobytes.cybermanaver2.components.injectionParametre.InjectionParametreComponent
import com.terrobytes.cybermanaver2.components.login.DefaultLoginComponent
import com.terrobytes.cybermanaver2.components.login.LoginComponent
import com.terrobytes.cybermanaver2.components.manuallyConnection.DefaultManuallyConnexionComponent
import com.terrobytes.cybermanaver2.components.manuallyConnection.ManuallyConnexionComponent
import com.terrobytes.cybermanaver2.components.parametrageReseaux.DefaultParametrageReseauxComponent
import com.terrobytes.cybermanaver2.components.parametrageReseaux.ParametrageReseauxComponent
import com.terrobytes.cybermanaver2.network.MikrotikSessionManager
import com.terrobytes.cybermanaver2.network.NetworkTarget
import kotlinx.serialization.Serializable


interface RootComponent {

    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Detection(val component: DetectionComponent) : Child()
        class Login(val component: LoginComponent) : Child()
        class ManuallyConnexion(val component: ManuallyConnexionComponent) : Child()
        class ParametrageReseaux(val component: ParametrageReseauxComponent) : Child()
        class InjectionParametre(val component: InjectionParametreComponent) : Child()
        class Dashboard(val component: DashboardComponent) : Child()
    }

}

@Serializable
private sealed class Config {

    @Serializable
    data object Detection : Config()

    @Serializable
    data class Login(val ipAddress : String, val name : String) : Config()

    @Serializable
    data object ManuallyConnexion : Config()

    @Serializable
    data class ParametrageReseaux(val ssid24 : String, val ssid5 : String) : Config()

    @Serializable
    data class InjectionParameter(
        val ssid24 : String,
        val ssid5 : String,
        val wifiPassword : String,
        val adminCount : Int,
        val lanBase : String,
        val lanCidr : String,
        val routerIp : String,
        val dhcpPoolStart : String,
        val dhcpPoolEnd : String
    ) : Config()
    
    @Serializable
    data class Dashboard(val host: String) : Config()

}

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()
    private val sessionManager = instanceKeeper.getOrCreate { MikrotikSessionManager() }
    private var pendingNetworkTarget: NetworkTarget? = null

    private val _childStack : Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Detection,
        handleBackButton = true,
        childFactory = ::createChild
    )

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = _childStack

    @OptIn(DelicateDecomposeApi::class)
    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ) : RootComponent.Child =
        when (config) {

            is Config.Detection -> RootComponent.Child.Detection(
                DefaultDetectionComponent(
                    componentContext = componentContext,
                    sessionManager = sessionManager,
                    onConnectClicked = { routeur ->
                        pendingNetworkTarget = routeur.networkTarget
                        navigation.push(Config.Login(routeur.ipAddress, routeur.name))
                    },
                    onFirstParameter = { routeur ->
                        navigation.push(Config.ParametrageReseaux(
                            routeur.ssid24 ?: "",
                            routeur.ssid5 ?: "",
                        ))
                    },
                    onConnectManuallyClicked = { navigation.push(Config.ManuallyConnexion)}
                )
            )

            is Config.Login -> RootComponent.Child.Login(
                DefaultLoginComponent(
                    componentContext = componentContext,
                    sessionManager = sessionManager,
                    ipAddress = config.ipAddress,
                    name = config.name,
                    networkTarget = pendingNetworkTarget,
                    onBackClicked = { navigation.pop() },
                    onSubmitClicked = { routeur ->
                        navigation.push(Config.ParametrageReseaux(
                            routeur.ssid24 ?: "",
                            routeur.ssid5 ?: "")
                        )
                    }
                )
            )

            is Config.ManuallyConnexion -> RootComponent.Child.ManuallyConnexion(
                DefaultManuallyConnexionComponent(
                    componentContext = componentContext,
                    sessionManager = sessionManager,
                    onGoBackClicked = { navigation.pop() },
                )
            )

            is Config.ParametrageReseaux -> RootComponent.Child.ParametrageReseaux(
                DefaultParametrageReseauxComponent(
                    ssid24 = config.ssid24,
                    ssid5 = config.ssid5,
                    componentContext = componentContext,
                    sessionManager = sessionManager,
                    onBackClicked = { navigation.pop() },
                    onContinueClicked = { template ->
                        navigation.push(
                            Config.InjectionParameter(
                                ssid24 = template.ssid24,
                                ssid5 = template.ssid5,
                                wifiPassword = template.wifiPassword,
                                adminCount = template.adminCount,
                                lanBase = template.lanBase,
                                lanCidr = template.lanCidr,
                                routerIp = template.routerIp,
                                dhcpPoolStart = template.dhcpPoolStart,
                                dhcpPoolEnd = template.dhcpPoolEnd
                            )
                        )
                    }
                )
            )

            is Config.InjectionParameter -> RootComponent.Child.InjectionParametre(
                DefaultInjectionParametreComponent(
                    componentContext = componentContext,
                    host = TODO(),
                    adminUsername = TODO(),
                    adminPassword = TODO(),
                    template = TODO(),
                    sessionManager = TODO(),
                    wifiConnector = TODO(),
                    onBackClicked = TODO(),
                    onInjectionSuccess = TODO(),
                )
            )

            is Config.Dashboard -> RootComponent.Child.Dashboard(
                DefaultDashboardComponent(
                    componentContext = componentContext,
                )
            )

        }

}