package org.arrowx.vpn.presentation.main

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.arrowx.vpn.R
import org.arrowx.vpn.data.repository.ArrowRepository
import org.arrowx.vpn.domain.model.ConnectionMode
import org.arrowx.vpn.domain.model.ConnectionStatus
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.domain.model.StartupData

class MainViewModel(
    private val repository: ArrowRepository,
    appContext: Context
) : ViewModel() {
    private val applicationContext = appContext.applicationContext
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MainUiEffect>()
    val effects: SharedFlow<MainUiEffect> = _effects.asSharedFlow()

    private var timerJob: Job? = null
    private var timerBase = 0L
    private var connectTransitionJob: Job? = null
    private var disconnectTransitionJob: Job? = null

    fun initialize(startupData: StartupData) {
        if (_uiState.value.initialized) return
        val selectedServer = startupData.selectedServerId
            ?.takeIf { selected -> startupData.servers.any { it.id == selected } }
            ?: startupData.servers.firstOrNull()?.id

        _uiState.value = MainUiState(
            initialized = true,
            isLoggedIn = startupData.isLoggedIn,
            loginUuid = startupData.uuid,
            loginPassword = startupData.password,
            currentUserUuid = startupData.uuid,
            serverNodes = startupData.servers,
            selectedServerId = selectedServer,
            connectionMode = startupData.settings.connectionMode,
            appSettings = startupData.settings
        )
        refreshPings()
        if (startupData.settings.autoConnect || startupData.settings.killSwitch) {
            requestConnectionIfEligible(
                statusText = if (startupData.settings.killSwitch) {
                    stringRes(R.string.status_kill_switch_starting)
                } else {
                    stringRes(R.string.status_auto_connect_starting)
                }
            )
        }
    }

    fun onLoginUuidChanged(value: String) {
        _uiState.update { it.copy(loginUuid = value) }
    }

    fun onLoginPasswordChanged(value: String) {
        _uiState.update { it.copy(loginPassword = value) }
    }

    fun onOpenLogin() {
        _uiState.update {
            it.copy(
                showLoginOverlay = true,
                loginTitle = stringRes(R.string.login_title_credentials),
                loginTitleIsError = false,
                loginButtonEnabled = true,
                loginButtonText = stringRes(R.string.login_button_verify_access)
            )
        }
    }

    fun onDismissLogin() {
        _uiState.update { it.copy(showLoginOverlay = false) }
    }

    fun onLoginSubmit() {
        val state = _uiState.value
        val uuid = state.loginUuid.trim()
        val password = state.loginPassword.trim()
        if (uuid.isEmpty() || password.isEmpty()) {
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_enter_all_fields), isError = true))
            return
        }

        _uiState.update {
            it.copy(
                loginTitle = stringRes(R.string.login_title_validating_access),
                loginTitleIsError = false,
                loginButtonText = stringRes(R.string.login_button_verifying),
                loginButtonEnabled = false
            )
        }

        viewModelScope.launch {
            val result = repository.login(uuid, password)
            if (result.isValid) {
                val servers = result.servers.ifEmpty { _uiState.value.serverNodes }
                val selectedServerId = _uiState.value.selectedServerId
                    ?.takeIf { current -> servers.any { it.id == current } }
                    ?: servers.firstOrNull()?.id

                if (!selectedServerId.isNullOrBlank()) {
                    repository.saveSelectedServer(selectedServerId)
                }

                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        showLoginOverlay = false,
                        loginTitle = stringRes(R.string.login_title_credentials),
                        loginTitleIsError = false,
                        loginButtonText = stringRes(R.string.login_button_verify_access),
                        loginButtonEnabled = true,
                        currentUserUuid = uuid,
                        serverNodes = servers,
                        selectedServerId = selectedServerId
                    )
                }
                emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_access_verified)))
                refreshPings()
                if (_uiState.value.appSettings.autoConnect || _uiState.value.appSettings.killSwitch) {
                    requestConnectionIfEligible(
                        statusText = if (_uiState.value.appSettings.killSwitch) {
                            stringRes(R.string.status_kill_switch_starting)
                        } else {
                            stringRes(R.string.status_auto_connect_starting)
                        }
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        loginTitle = stringRes(R.string.login_title_error_format, result.message),
                        loginTitleIsError = true,
                        loginButtonText = stringRes(R.string.login_button_verify_access),
                        loginButtonEnabled = true
                    )
                }
            }
        }
    }

    fun onModeSelected(mode: ConnectionMode) {
        val state = _uiState.value
        if (state.connectionStatus != ConnectionStatus.DISCONNECTED) {
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_disconnect_to_change_mode), isError = true))
            return
        }
        val updatedSettings = state.appSettings.copy(connectionMode = mode)
        repository.saveSettings(updatedSettings)
        _uiState.update {
            it.copy(
                connectionMode = mode,
                appSettings = updatedSettings
            )
        }
    }

    fun onPowerTapped() {
        val state = _uiState.value
        when (state.connectionStatus) {
            ConnectionStatus.CONNECTING,
            ConnectionStatus.DISCONNECTING -> Unit

            ConnectionStatus.DISCONNECTED -> {
                if (!state.isLoggedIn) {
                    onOpenLogin()
                    emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_login_first), isError = true))
                    return
                }
                requestConnectionIfEligible()
            }

            ConnectionStatus.CONNECTED -> {
                if (state.appSettings.killSwitch) {
                    emitEffect(
                        MainUiEffect.ShowToast(
                            stringRes(R.string.toast_kill_switch_disable_to_disconnect),
                            isError = true
                        )
                    )
                    return
                }
                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.DISCONNECTING,
                        statusText = stringRes(R.string.status_stopping_servers)
                    )
                }
                emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_stopping_servers)))
                disconnectTransitionJob?.cancel()
                disconnectTransitionJob = viewModelScope.launch {
                    delay(900)
                    emitEffect(MainUiEffect.StopVpn)
                }
            }
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        val state = _uiState.value
        if (state.connectionStatus != ConnectionStatus.CONNECTING) return

        if (!granted) {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    statusText = stringRes(R.string.status_protection_inactive)
                )
            }
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_vpn_permission_denied), isError = true))
            return
        }

        val selectedServer = state.serverNodes.firstOrNull { it.id == state.selectedServerId }
        if (selectedServer == null) {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    statusText = stringRes(R.string.status_protection_inactive)
                )
            }
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_no_server_selected), isError = true))
            return
        }

        emitEffect(MainUiEffect.StartVpn(selectedServer.vlessConfig))
        connectTransitionJob?.cancel()
        connectTransitionJob = viewModelScope.launch {
            delay(900)
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    statusText = if (it.connectionMode == ConnectionMode.VPN) {
                        stringRes(R.string.status_vpn_active)
                    } else {
                        stringRes(R.string.status_proxy_active)
                    }
                )
            }
            startTimer()
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_connection_established)))
        }
    }

    fun onVpnStopped() {
        val shouldReconnect =
            _uiState.value.appSettings.killSwitch &&
                _uiState.value.isLoggedIn &&
                !_uiState.value.selectedServerId.isNullOrBlank()
        stopTimer()
        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                statusText = stringRes(R.string.status_protection_inactive),
                elapsedText = ""
            )
        }
        if (shouldReconnect) {
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_kill_switch_reconnecting), isError = true))
            requestConnectionIfEligible(statusText = stringRes(R.string.toast_kill_switch_reconnecting))
        } else {
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.status_protection_inactive), isError = true))
        }
    }

    fun onToggleServerMenu() {
        _uiState.update { it.copy(showServerMenu = !it.showServerMenu) }
    }

    fun onServerSelected(serverId: String) {
        val state = _uiState.value
        if (state.connectionStatus != ConnectionStatus.DISCONNECTED) {
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_disconnect_to_change_server), isError = true))
            return
        }
        repository.saveSelectedServer(serverId)
        _uiState.update {
            it.copy(
                selectedServerId = serverId,
                showServerMenu = false
            )
        }
    }

    fun refreshPings() {
        if (_uiState.value.serverNodes.isEmpty()) return
        _uiState.update { it.copy(isPinging = true) }
        viewModelScope.launch {
            val latencies = repository.measureLatencies(_uiState.value.serverNodes)
            _uiState.update {
                it.copy(
                    serverPings = latencies,
                    isPinging = false
                )
            }
        }
    }

    fun onOpenSupportPanel() {
        _uiState.update {
            it.copy(
                showSupportPanel = true,
                showSettingsPanel = false,
                showCredentialsPanel = false
            )
        }
    }

    fun onOpenSettingsPanel() {
        _uiState.update {
            it.copy(
                showSupportPanel = false,
                showSettingsPanel = true,
                showCredentialsPanel = false
            )
        }
    }

    fun onOpenCredentialsPanel() {
        _uiState.update {
            it.copy(
                showSupportPanel = false,
                showSettingsPanel = false,
                showCredentialsPanel = true
            )
        }
    }

    fun onBackToSettingsPanel() {
        _uiState.update {
            it.copy(
                showSupportPanel = false,
                showSettingsPanel = true,
                showCredentialsPanel = false
            )
        }
    }

    fun onClosePanels() {
        _uiState.update {
            it.copy(
                showSupportPanel = false,
                showSettingsPanel = false,
                showCredentialsPanel = false
            )
        }
    }

    fun onToggleTray(enabled: Boolean) {
        updateSettings { copy(minimizeToTray = enabled) }
    }

    fun onToggleAutoConnect(enabled: Boolean) {
        updateSettings { copy(autoConnect = enabled) }
        if (enabled) {
            requestConnectionIfEligible(statusText = stringRes(R.string.status_auto_connect_starting))
        }
    }

    fun onToggleKillSwitch(enabled: Boolean) {
        updateSettings { copy(killSwitch = enabled) }
        if (enabled) {
            if (_uiState.value.isLoggedIn) {
                requestConnectionIfEligible(statusText = stringRes(R.string.status_kill_switch_starting))
            } else {
                emitEffect(
                    MainUiEffect.ShowToast(
                        stringRes(R.string.toast_enable_kill_switch_requires_login),
                        isError = true
                    )
                )
            }
        }
    }

    fun onLogout() {
        val wasConnected = _uiState.value.connectionStatus == ConnectionStatus.CONNECTED
        if (wasConnected) {
            emitEffect(MainUiEffect.StopVpn)
        }
        stopTimer()
        repository.logout()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                currentUserUuid = "",
                connectionStatus = ConnectionStatus.DISCONNECTED,
                statusText = stringRes(R.string.status_protection_inactive),
                elapsedText = "",
                showSupportPanel = false,
                showSettingsPanel = false,
                showCredentialsPanel = false
            )
        }
        emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_session_closed)))
    }

    fun onCopySessionId() {
        emitEffect(
            MainUiEffect.CopyToClipboard(
                value = SESSION_ID,
                successMessage = stringRes(R.string.toast_copy_session_id_success)
            )
        )
    }

    fun onCopySupportEmail() {
        emitEffect(
            MainUiEffect.CopyToClipboard(
                value = SUPPORT_EMAIL,
                successMessage = stringRes(R.string.toast_copy_support_email_success)
            )
        )
    }

    fun onCopySubscriptionLink() {
        val uuid = _uiState.value.currentUserUuid
        if (uuid.isBlank()) {
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_no_active_session), isError = true))
            return
        }
        emitEffect(
            MainUiEffect.CopyToClipboard(
                value = repository.buildSubscriptionLink(uuid),
                successMessage = stringRes(R.string.toast_copy_subscription_success)
            )
        )
    }

    fun onCopyServerVless(server: ServerNode) {
        emitEffect(
            MainUiEffect.CopyToClipboard(
                value = server.vlessConfig,
                successMessage = stringRes(R.string.toast_copy_vless_success, server.name)
            )
        )
    }

    private fun startTimer() {
        timerBase = SystemClock.elapsedRealtime()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - timerBase
                val seconds = (elapsed / 1_000) % 60
                val minutes = (elapsed / 60_000) % 60
                val hours = elapsed / 3_600_000
                _uiState.update {
                    it.copy(
                        elapsedText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    )
                }
                delay(1_000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun requestConnectionIfEligible(statusText: String? = null): Boolean {
        val state = _uiState.value
        if (state.connectionStatus != ConnectionStatus.DISCONNECTED) return false
        if (!state.isLoggedIn) return false
        if (state.selectedServerId.isNullOrBlank()) {
            emitEffect(MainUiEffect.ShowToast(stringRes(R.string.toast_no_server_selected), isError = true))
            return false
        }

        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.CONNECTING,
                statusText = statusText ?: defaultConnectingStatusText(it.connectionMode)
            )
        }
        emitEffect(MainUiEffect.RequestVpnPermission)
        return true
    }

    private fun defaultConnectingStatusText(mode: ConnectionMode): String {
        return if (mode == ConnectionMode.VPN) {
            stringRes(R.string.status_starting_tun)
        } else {
            stringRes(R.string.status_starting_proxy)
        }
    }

    private fun stringRes(resId: Int): String = applicationContext.getString(resId)

    private fun stringRes(resId: Int, vararg formatArgs: Any): String =
        applicationContext.getString(resId, *formatArgs)

    private fun updateSettings(update: org.arrowx.vpn.domain.model.AppSettings.() -> org.arrowx.vpn.domain.model.AppSettings) {
        val updated = _uiState.value.appSettings.update()
        repository.saveSettings(updated)
        _uiState.update {
            it.copy(
                appSettings = updated,
                connectionMode = updated.connectionMode
            )
        }
    }

    private fun emitEffect(effect: MainUiEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    companion object {
        private const val SESSION_ID = "05900ca5fcec8fe58b509961f5ed9c658a876c1308b9bfdd69e14effbd1ed13a0e"
        private const val SUPPORT_EMAIL = "support@arrow-x.org"
    }
}
