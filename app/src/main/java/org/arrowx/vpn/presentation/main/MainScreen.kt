package org.arrowx.vpn.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.arrowx.vpn.R
import org.arrowx.vpn.domain.model.ConnectionMode
import org.arrowx.vpn.domain.model.ConnectionStatus
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.presentation.components.FloatingPanels
import org.arrowx.vpn.presentation.components.HeaderBar
import org.arrowx.vpn.presentation.components.LoginOverlay
import org.arrowx.vpn.presentation.components.ModeSelector
import org.arrowx.vpn.presentation.components.PowerButton
import org.arrowx.vpn.presentation.components.ServerSelectorCard

@Composable
fun MainScreen(
    uiState: MainUiState,
    appVersion: String,
    onPowerTapped: () -> Unit,
    onModeSelected: (ConnectionMode) -> Unit,
    onToggleServerMenu: () -> Unit,
    onSelectServer: (String) -> Unit,
    onRefreshPings: () -> Unit,
    onOpenLogin: () -> Unit,
    onDismissLogin: () -> Unit,
    onLoginUuidChanged: (String) -> Unit,
    onLoginPasswordChanged: (String) -> Unit,
    onLoginSubmit: () -> Unit,
    onOpenSupportPanel: () -> Unit,
    onOpenSettingsPanel: () -> Unit,
    onOpenCredentialsPanel: () -> Unit,
    onBackToSettingsPanel: () -> Unit,
    onClosePanels: () -> Unit,
    onToggleAutoConnect: (Boolean) -> Unit,
    onToggleKillSwitch: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onCopySessionId: () -> Unit,
    onCopySupportEmail: () -> Unit,
    onCopySubscription: () -> Unit,
    onCopyServerVless: (ServerNode) -> Unit
) {
    val anyPanelOpen = uiState.showSupportPanel || uiState.showSettingsPanel || uiState.showCredentialsPanel
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (anyPanelOpen) 8.dp else 0.dp)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderBar(
                onHelpClick = onOpenSupportPanel,
                onSettingsClick = onOpenSettingsPanel
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ModeSelector(
                            selectedMode = uiState.connectionMode,
                            onModeSelected = onModeSelected
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.statusText,
                            color = when (uiState.connectionStatus) {
                                ConnectionStatus.CONNECTED, ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.secondary
                                ConnectionStatus.DISCONNECTING -> MaterialTheme.colorScheme.error
                                ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Keep it for later ;)
//                        if (uiState.elapsedText.isNotBlank()) {
//                            Text(
//                                text = uiState.elapsedText,
//                                color = ArrowSecondaryText,
//                                fontSize = 11.sp
//                            )
//                        }
                        Spacer(modifier = Modifier.height(22.dp))
                        PowerButton(
                            status = uiState.connectionStatus,
                            onTap = onPowerTapped
                        )
                        Spacer(modifier = Modifier.height(36.dp))
                        ServerSelectorCard(
                            servers = uiState.serverNodes,
                            selectedServerId = uiState.selectedServerId,
                            pings = uiState.serverPings,
                            expanded = uiState.showServerMenu,
                            isPinging = uiState.isPinging,
                            onToggleExpanded = onToggleServerMenu,
                            onSelectServer = onSelectServer,
                            onRefreshPings = onRefreshPings
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.isLoggedIn) {
                                stringResource(R.string.session_active)
                            } else {
                                stringResource(R.string.session_inactive)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        if (!uiState.isLoggedIn) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.action_login),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(onClick = onOpenLogin)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        FloatingPanels(
            showSupportPanel = uiState.showSupportPanel,
            showSettingsPanel = uiState.showSettingsPanel,
            showCredentialsPanel = uiState.showCredentialsPanel,
            settings = uiState.appSettings,
            appVersion = appVersion,
            servers = uiState.serverNodes,
            onClose = onClosePanels,
            onOpenCredentials = onOpenCredentialsPanel,
            onBackToSettings = onBackToSettingsPanel,
            onToggleAutoConnect = onToggleAutoConnect,
            onToggleKillSwitch = onToggleKillSwitch,
            onLogout = onLogout,
            onCopySessionId = onCopySessionId,
            onCopySupportEmail = onCopySupportEmail,
            onCopySubscription = onCopySubscription,
            onCopyServerVless = onCopyServerVless
        )

        if (uiState.showLoginOverlay) {
            LoginOverlay(
                title = uiState.loginTitle,
                titleIsError = uiState.loginTitleIsError,
                uuid = uiState.loginUuid,
                password = uiState.loginPassword,
                buttonText = uiState.loginButtonText,
                buttonEnabled = uiState.loginButtonEnabled,
                onUuidChange = onLoginUuidChanged,
                onPasswordChange = onLoginPasswordChanged,
                onSubmit = onLoginSubmit,
                onCancel = onDismissLogin
            )
        }
    }
}
