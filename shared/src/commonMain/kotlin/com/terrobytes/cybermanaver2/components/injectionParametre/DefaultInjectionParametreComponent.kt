package com.terrobytes.cybermanaver2.components.injectionParametre

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.terrobytes.cybermanaver2.network.*
import com.terrobytes.cybermanaver2.templates.CyberTemplateParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DefaultInjectionParametreComponent(
    componentContext: ComponentContext,
    private val host: String,
    private val adminUsername: String,
    private val adminPassword: String,
    private val template: CyberTemplateParams,
    private val sessionManager: MikrotikSessionManager,
    private val wifiConnector: WifiConnector,
    private val onBackClicked: () -> Unit,
    private val onInjectionSuccess: (connectedHost: String) -> Unit,
) : InjectionParametreComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val backupManager = RouterBackupManager()
    private val injectionManager = RouterInjectionManager()

    private val _uiState = MutableValue(InjectionUiState())
    override val uiState: Value<InjectionUiState> = _uiState

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        runFlow()
    }

    override fun onRetry() = runFlow()

    override fun onCancel() = onBackClicked()

    private fun runFlow() {
        _uiState.value = InjectionUiState()

        scope.launch {
            val backupResult = backupManager.backupRouter(
                host = host,
                username = adminUsername,
                password = adminPassword,
                sessionManager = sessionManager,
                onStep = { step -> pushBackupStep(step) },
            )

            val backup = backupResult.getOrNull()
            if (backup == null) {
                _uiState.value = _uiState.value.copy(isRunning = false, failed = true)
                return@launch
            }

            val uploadResult = injectionManager.uploadAndReset(
                host = host,
                adminUsername = adminUsername,
                adminPassword = adminPassword,
                template = template,
                backupPassword = backup.binaryBackupPassword,
                sessionManager = sessionManager,
                onStep = { step -> pushInjectionStep(step) },
            )

            val apPassword = uploadResult.getOrNull()
            if (apPassword == null) {
                _uiState.value = _uiState.value.copy(isRunning = false, failed = true)
                return@launch
            }

            val verifyResult = injectionManager.reconnectAndVerify(
                host = host,
                apPassword = apPassword,
                wifiConnector = wifiConnector,
                ssid = template.ssid24,
                wifiPassword = template.wifiPassword,
                onStep = { step -> pushInjectionStep(step) },
                sessionManager = sessionManager,
            )

            val connectedHost = verifyResult.getOrNull()
            _uiState.value = _uiState.value.copy(isRunning = false, failed = connectedHost == null)
            if (connectedHost != null) {
                onInjectionSuccess(connectedHost)
            }
        }
    }

    private fun pushBackupStep(step: BackupStep) {
        val current = _uiState.value
        _uiState.value = current.copy(
            phase = InjectionPhase.Backup(step),
            logs = current.logs + backupLabel(step),
            failed = step is BackupStep.Failed,
        )
    }

    private fun pushInjectionStep(step: InjectionStep) {
        val current = _uiState.value
        _uiState.value = current.copy(
            phase = InjectionPhase.Inject(step),
            logs = current.logs + injectionLabel(step),
            failed = step is InjectionStep.Failed || step is InjectionStep.GaveUpWaiting,
        )
    }

    private fun backupLabel(step: BackupStep): String = when (step) {
        BackupStep.Connecting -> "Connexion au routeur…"
        BackupStep.ReadingIdentity -> "Lecture de l'identité du routeur…"
        BackupStep.ExportingConfig -> "Export de la configuration…"
        BackupStep.SavingBinaryBackup -> "Sauvegarde binaire en cours…"
        BackupStep.DownloadingFiles -> "Téléchargement des fichiers de sauvegarde…"
        BackupStep.Persisting -> "Enregistrement local de la sauvegarde…"
        BackupStep.Done -> "Sauvegarde terminée"
        is BackupStep.Failed -> "Échec de la sauvegarde : ${step.reason}"
    }

    private fun injectionLabel(step: InjectionStep): String = when (step) {
        InjectionStep.UploadingScript -> "Envoi du script de configuration…"
        InjectionStep.TriggeringReset -> "Déclenchement de la réinitialisation…"
        InjectionStep.WaitingForReboot -> "Redémarrage du routeur en cours…"
        InjectionStep.EnsuringWifiEnabled -> "Vérification du Wi-Fi de l'appareil…"
        InjectionStep.ConnectingToWifi -> "Connexion au réseau du routeur…"
        is InjectionStep.Reconnecting -> "Tentative de reconnexion (${step.attempt})…"
        InjectionStep.Verifying -> "Vérification de la configuration…"
        InjectionStep.CancellingFailsafe -> "Annulation du filet de sécurité…"
        InjectionStep.Done -> "Configuration terminée avec succès"
        is InjectionStep.Failed -> "Échec : ${step.reason}"
        InjectionStep.GaveUpWaiting -> "Délai dépassé, restauration automatique en cours"
    }
}