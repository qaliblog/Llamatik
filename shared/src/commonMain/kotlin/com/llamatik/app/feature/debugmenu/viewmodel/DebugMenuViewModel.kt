package com.llamatik.app.feature.debugmenu.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import com.llamatik.app.feature.debugmenu.repositories.GlobalAppSettingsRepository
import com.llamatik.app.feature.server.ServerManager
import com.llamatik.app.feature.server.ServerProvider
import com.llamatik.app.localization.AvailableLanguages
import com.llamatik.app.platform.Environment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class DebugMenuViewModel(
    private val globalAppSettingsRepository: GlobalAppSettingsRepository
) : ScreenModel {
    private val _state = MutableStateFlow(
        DebugMenuState(
            currentEnvironment = globalAppSettingsRepository.getCurrentEnvironment(),
            currentLanguage = globalAppSettingsRepository.getCurrentLanguage(),
            isMockedContentChecked = globalAppSettingsRepository.isMockedContentEnabled(),
            isMockedUserChecked = globalAppSettingsRepository.isMockedUserEnabled(),
            isServerRunning = ServerManager.isRunning(),
            serverPort = 8080,
            serverProvider = ServerProvider.BOTH
        )
    )
    val state = _state.asStateFlow()
    private val _sideEffects = Channel<DebugMenuSideEffects>()
    val sideEffects: Flow<DebugMenuSideEffects> = _sideEffects.receiveAsFlow()

    fun onSelectedEnvironment(environment: Environment) {
        _state.value = _state.value.copy(currentEnvironment = environment)
        globalAppSettingsRepository.setSelectedEnvironment(environment)
    }

    fun onSelectedLanguage(language: AvailableLanguages) {
        _state.value = _state.value.copy(currentLanguage = language)
        globalAppSettingsRepository.setSelectedLanguage(language)
    }

    fun onMockedContentCheckChanged(checked: Boolean) {
        _state.value = _state.value.copy(isMockedContentChecked = checked)
        globalAppSettingsRepository.setMockedContentCheckStatus(checked)
    }

    fun onServerToggle(checked: Boolean) {
        if (checked) {
            ServerManager.startServer(_state.value.serverPort, _state.value.serverProvider)
        } else {
            ServerManager.stopServer()
        }
        _state.value = _state.value.copy(isServerRunning = checked)
    }

    fun onServerPortChanged(port: Int) {
        _state.value = _state.value.copy(serverPort = port)
    }

    fun onServerProviderChanged(provider: ServerProvider) {
        _state.value = _state.value.copy(serverProvider = provider)
    }
}

data class DebugMenuState(
    val currentEnvironment: Environment,
    val currentLanguage: AvailableLanguages,
    val isMockedContentChecked: Boolean,
    val isMockedUserChecked: Boolean,
    val isServerRunning: Boolean,
    val serverPort: Int,
    val serverProvider: ServerProvider
)

sealed class DebugMenuSideEffects {
    data object Initial : DebugMenuSideEffects()
}
