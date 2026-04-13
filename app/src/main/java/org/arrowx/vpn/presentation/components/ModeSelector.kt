package org.arrowx.vpn.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.arrowx.vpn.R
import org.arrowx.vpn.domain.model.ConnectionMode

@Composable
fun ModeSelector(
    selectedMode: ConnectionMode,
    onModeSelected: (ConnectionMode) -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        ModePill(
            label = stringResource(R.string.mode_vpn),
            active = selectedMode == ConnectionMode.VPN,
            onClick = { onModeSelected(ConnectionMode.VPN) }
        )
        ModePill(
            label = stringResource(R.string.mode_proxy),
            active = selectedMode == ConnectionMode.PROXY,
            onClick = { onModeSelected(ConnectionMode.PROXY) }
        )
    }
}

@Composable
private fun ModePill(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .background(
                color = if (active) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
}
