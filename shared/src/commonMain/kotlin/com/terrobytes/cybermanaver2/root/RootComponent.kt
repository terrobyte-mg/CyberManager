package com.terrobytes.cybermanaver2.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.terrobytes.cybermanaver2.components.detectionRouteur.DefaultDetectionComponent
import com.terrobytes.cybermanaver2.components.detectionRouteur.DetectionComponent
import com.terrobytes.cybermanaver2.components.login.DefaultLoginComponent
import com.terrobytes.cybermanaver2.components.login.LoginComponent
import com.terrobytes.cybermanaver2.components.manuallyConnection.DefaultManuallyConnexionComponent
import com.terrobytes.cybermanaver2.components.manuallyConnection.ManuallyConnexionComponent
import com.terrobytes.cybermanaver2.network.NetworkTarget
import kotlinx.serialization.Serializable


interface RootComponent {

    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Detection(val component: DetectionComponent) : Child()
        class Login(val component: LoginComponent) : Child()
        class ManuallyConnexion(val component: ManuallyConnexionComponent) : Child()
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

}

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    // NetworkTarget carries a platform-specific handle (android.net.Network on
    // Android) that kotlinx.serialization can't put in Config, so it can't
    // travel through navigation the normal way. We stash the most recently
    // selected one here and hand it to the Login screen directly.
    // Trade-off: this is a plain var, not restored across process death - if
    // the process is killed while on the Login screen, this comes back null
    // and MikrotikRawClient just falls back to an unbound socket instead of
    // crashing.
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
                    onConnectClicked = { routeur ->
                        pendingNetworkTarget = routeur.networkTarget
                        navigation.push(Config.Login(routeur.ipAddress, routeur.name))
                    },
                    onConnectManuallyClicked = { navigation.push(Config.ManuallyConnexion)}
                )
            )

            is Config.Login -> RootComponent.Child.Login(
                DefaultLoginComponent(
                    componentContext = componentContext,
                    ipAddress = config.ipAddress,
                    name = config.name,
                    networkTarget = pendingNetworkTarget,
                    onBackClicked = { navigation.pop() },
                )
            )

            is Config.ManuallyConnexion -> RootComponent.Child.ManuallyConnexion(
                DefaultManuallyConnexionComponent(
                    componentContext = componentContext,
                    onGoBackClicked = { navigation.pop() },
                )
            )

        }

}