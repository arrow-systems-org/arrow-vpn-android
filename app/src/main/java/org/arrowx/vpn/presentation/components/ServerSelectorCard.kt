package org.arrowx.vpn.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.arrowx.vpn.R
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.domain.model.toFlagEmoji

@Composable
fun ServerSelectorCard(
    servers: List<ServerNode>,
    selectedServerId: String?,
    pings: Map<String, Long?>,
    expanded: Boolean,
    isPinging: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectServer: (String) -> Unit,
    onRefreshPings: () -> Unit
) {
    val selected = servers.firstOrNull { it.id == selectedServerId }
    val serverListScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.server_current_location),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServerFlag(
                    countryCode = selected?.countryCode.orEmpty()
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = selected?.name ?: stringResource(R.string.server_loading_nodes),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) {
                    stringResource(R.string.cd_close_server_list)
                } else {
                    stringResource(R.string.cd_open_server_list)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPinging) {
                        stringResource(R.string.server_live_latency_updating)
                    } else {
                        stringResource(R.string.server_live_latency)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh_latencies),
                    tint = if (isPinging) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = !isPinging, onClick = onRefreshPings)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(serverListScrollState)
                        .padding(end = 8.dp)
                ) {
                    servers.forEach { server ->
                        val ping = pings[server.id]
                        val isDown = ping == null
                        val pingText = if (isDown) {
                            stringResource(R.string.server_out_of_service)
                        } else {
                            stringResource(R.string.server_ping_ms, ping ?: 0L)
                        }
                        val statusColor = when {
                            ping == null -> MaterialTheme.colorScheme.error
                            ping <= 199 -> MaterialTheme.colorScheme.secondary
                            ping <= 800 -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.error
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (selectedServerId == server.id) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !isDown) { onSelectServer(server.id) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ServerFlag(countryCode = server.countryCode)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = server.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = pingText,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        lineHeight = 1.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerFlag(
    countryCode: String
) {
//    Box(
//        modifier = Modifier
//            .size(18.dp),
//        contentAlignment = Alignment.Center,
//    ) {
        Text(
            text = countryCode.toFlagEmoji(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
//    }
}
