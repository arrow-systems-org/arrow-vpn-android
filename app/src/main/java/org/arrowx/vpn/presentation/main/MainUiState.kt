package org.arrowx.vpn.presentation.main

import org.arrowx.vpn.domain.model.AppSettings
import org.arrowx.vpn.domain.model.ConnectionMode
import org.arrowx.vpn.domain.model.ConnectionStatus
import org.arrowx.vpn.domain.model.ServerNode

data class MainUiState(
    val initialized: Boolean = false,
    val isLoggedIn: Boolean = false,
    val showLoginOverlay: Boolean = false,
    val loginTitle: String = "",
    val loginTitleIsError: Boolean = false,
    val loginButtonText: String = "",
    val loginButtonEnabled: Boolean = true,
    val loginUuid: String = "",
    val loginPassword: String = "",
    val currentUserUuid: String = "",
    val serverNodes: List<ServerNode> = emptyList(),
    val selectedServerId: String? = null,
    val serverPings: Map<String, Long?> = emptyMap(),
    val isPinging: Boolean = false,
    val showServerMenu: Boolean = false,
    val connectionMode: ConnectionMode = ConnectionMode.VPN,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val statusText: String = "",
    val elapsedText: String = "",
    val showSupportPanel: Boolean = false,
    val showSettingsPanel: Boolean = false,
    val showCredentialsPanel: Boolean = false,
    val appSettings: AppSettings = AppSettings()
)
