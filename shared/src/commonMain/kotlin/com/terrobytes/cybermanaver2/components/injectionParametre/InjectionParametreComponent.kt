package com.terrobytes.cybermanaver2.components.injectionParametre

import com.arkivanov.decompose.value.Value
import com.terrobytes.cybermanaver2.network.BackupStep
import com.terrobytes.cybermanaver2.network.InjectionStep

sealed interface InjectionPhase {
    data class Backup(val step: BackupStep) : InjectionPhase
    data class Inject(val step: InjectionStep) : InjectionPhase
}

data class InjectionUiState(
    val phase: InjectionPhase = InjectionPhase.Backup(BackupStep.Connecting),
    val logs: List<String> = emptyList(),
    val isRunning: Boolean = true,
    val failed: Boolean = false,
)

interface InjectionParametreComponent {
    val uiState: Value<InjectionUiState>
    fun onRetry()
    fun onCancel()
}