package org.arrowx.vpn.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.arrowx.vpn.R
import org.arrowx.vpn.domain.model.AppSettings
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.domain.model.toFlagEmoji

private enum class PanelType { SUPPORT, SETTINGS, CREDENTIALS, NONE }

@Composable
fun FloatingPanels(
    showSupportPanel: Boolean,
    showSettingsPanel: Boolean,
    showCredentialsPanel: Boolean,
    settings: AppSettings,
    appVersion: String,
    servers: List<ServerNode>,
    onClose: () -> Unit,
    onOpenCredentials: () -> Unit,
    onBackToSettings: () -> Unit,
    onToggleAutoConnect: (Boolean) -> Unit,
    onToggleKillSwitch: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onCopySessionId: () -> Unit,
    onCopySupportEmail: () -> Unit,
    onCopySubscription: () -> Unit,
    onCopyServerVless: (ServerNode) -> Unit
) {
    val showAnyPanel = showSupportPanel || showSettingsPanel || showCredentialsPanel
    val activePanel = when {
        showSupportPanel -> PanelType.SUPPORT
        showSettingsPanel -> PanelType.SETTINGS
        showCredentialsPanel -> PanelType.CREDENTIALS
        else -> PanelType.NONE
    }
    val panelTransition = remember { MutableTransitionState(false) }
    LaunchedEffect(showAnyPanel) {
        panelTransition.targetState = showAnyPanel
    }
    if (!panelTransition.currentState && !panelTransition.targetState) return
    val scrimAlpha by animateFloatAsState(
        targetValue = if (showAnyPanel) 0.70f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "panelScrimAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visibleState = panelTransition,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(180))
        ) {
            AnimatedContent(
                targetState = activePanel,
                transitionSpec = {
                    (
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(220))
                        ).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180))
                    )
                },
                label = "panelContentSwitch"
            ) { shownPanel ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    horizontalAlignment = Alignment.Start
                ) {
                    when (shownPanel) {
                        PanelType.SUPPORT -> SupportPanel(
                            appVersion = appVersion,
                            onCopySessionId = onCopySessionId,
                            onCopySupportEmail = onCopySupportEmail,
                            onClose = onClose
                        )
                        PanelType.SETTINGS -> SettingsPanel(
                            settings = settings,
                            onOpenCredentials = onOpenCredentials,
                            onToggleAutoConnect = onToggleAutoConnect,
                            onToggleKillSwitch = onToggleKillSwitch,
                            onLogout = onLogout,
                            onClose = onClose
                        )
                        PanelType.CREDENTIALS -> CredentialsPanel(
                            servers = servers,
                            onCopySubscription = onCopySubscription,
                            onCopyServerVless = onCopyServerVless,
                            onBackToSettings = onBackToSettings
                        )
                        PanelType.NONE -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagThumbnail(
    countryCode: String
) {
    Text(
        text = countryCode.toFlagEmoji(),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SupportPanel(
    appVersion: String,
    onCopySessionId: () -> Unit,
    onCopySupportEmail: () -> Unit,
    onClose: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.support_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        SupportItem(
            title = stringResource(R.string.support_item_app_version),
            value = stringResource(R.string.support_app_version_value, appVersion),
            onClick = {}
        )
        Spacer(modifier = Modifier.height(6.dp))
        SupportItem(
            title = stringResource(R.string.support_item_session_id),
            value = stringResource(R.string.action_copy),
            onClick = onCopySessionId
        )
        Spacer(modifier = Modifier.height(6.dp))
        SupportItem(
            title = stringResource(R.string.support_item_help_center),
            value = stringResource(R.string.support_help_email),
            onClick = onCopySupportEmail,
            valueColor = MaterialTheme.colorScheme.tertiary,
            underlineValue = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        SecondaryButton(text = stringResource(R.string.action_close), onClick = onClose)
    }
}

@Composable
private fun SettingsPanel(
    settings: AppSettings,
    onOpenCredentials: () -> Unit,
    onToggleAutoConnect: (Boolean) -> Unit,
    onToggleKillSwitch: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.settings_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))
        SecondaryButton(text = stringResource(R.string.action_open_credentials), onClick = onOpenCredentials)
        Spacer(modifier = Modifier.height(5.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.error
            ),
            contentPadding = PaddingValues(vertical = 8.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.30f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.action_logout), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.settings_section_general),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
        ToggleRow(
            title = stringResource(R.string.settings_auto_connect),
            checked = settings.autoConnect,
            onCheckedChange = onToggleAutoConnect
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.settings_section_advanced_security),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
        ToggleRow(
            title = stringResource(R.string.settings_kill_switch),
            checked = settings.killSwitch,
            onCheckedChange = onToggleKillSwitch,
            titleColor = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(18.dp))
        SecondaryButton(text = stringResource(R.string.action_back), onClick = onClose)
    }
}

@Composable
private fun CredentialsPanel(
    servers: List<ServerNode>,
    onCopySubscription: () -> Unit,
    onCopyServerVless: (ServerNode) -> Unit,
    onBackToSettings: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.credentials_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.credentials_copy_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onCopySubscription,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.action_copy_subscription), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.credentials_section_vless_nodes),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 10.dp)
                    .verticalScroll(scrollState)
            ) {
                servers.forEach { server ->
                    VlessServerItem(
                        server = server,
                        onClick = { onCopyServerVless(server) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            ScrollIndicator(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
                containerHeight = 180.dp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        SecondaryButton(text = stringResource(R.string.action_back_to_settings), onClick = onBackToSettings)
    }
}

@Composable
private fun SupportItem(
    title: String,
    value: String,
    onClick: () -> Unit,
    valueColor: Color = Color.Unspecified,
    underlineValue: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(
            text = value,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = if (underlineValue) TextDecoration.Underline else null
        )
    }
}

@Composable
private fun VlessServerItem(
    server: ServerNode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlagThumbnail(countryCode = server.countryCode)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 1.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.credentials_copy_link),
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 1.sp
            )
        }
        Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun ScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    containerHeight: Dp
) {
    val thumbHeight = 36.dp
    val progress = if (scrollState.maxValue == 0) 0f else scrollState.value.toFloat() / scrollState.maxValue.toFloat()
    val maxOffset = containerHeight - thumbHeight
    Box(
        modifier = modifier
            .width(4.dp)
            .height(containerHeight)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(thumbHeight)
                .offset(y = maxOffset * progress)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    titleColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(220.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}
