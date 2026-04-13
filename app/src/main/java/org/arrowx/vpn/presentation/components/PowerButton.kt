package org.arrowx.vpn.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.arrowx.vpn.R
import org.arrowx.vpn.domain.model.ConnectionStatus

@Composable
fun PowerButton(
    status: ConnectionStatus,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isTransitioning = status == ConnectionStatus.CONNECTING || status == ConnectionStatus.DISCONNECTING
    val isProtected = status == ConnectionStatus.CONNECTED || status == ConnectionStatus.DISCONNECTING
    val transition = rememberInfiniteTransition(label = "powerVisualTransition")

    val iconBreathScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "powerIconBreathScale"
    )
    val transitionPulse by transition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920),
            repeatMode = RepeatMode.Reverse
        ),
        label = "powerTransitionPulse"
    )
    val idlePulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "powerIdlePulse"
    )

    val accentTarget = when (status) {
        ConnectionStatus.CONNECTING, ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.secondary
        ConnectionStatus.DISCONNECTING -> MaterialTheme.colorScheme.error
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val actionDescription = if (isProtected) {
        stringResource(R.string.cd_disconnect)
    } else {
        stringResource(R.string.cd_connect)
    }
    val accentColor by animateColorAsState(
        targetValue = accentTarget,
        animationSpec = tween(240),
        label = "powerButtonAccent"
    )

    val pulseFactor = if (isTransitioning) transitionPulse else idlePulse
    val containerScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(130),
        label = "powerButtonPressScale"
    )
    val coreShadowElevation by animateFloatAsState(
        targetValue = when {
            isPressed -> 8f
            isTransitioning -> 18f
            status == ConnectionStatus.CONNECTED -> 14f
            else -> 10f
        },
        animationSpec = tween(180),
        label = "powerCoreShadowElevation"
    )
    val haloShadowElevation by animateFloatAsState(
        targetValue = when {
            isTransitioning -> 26f
            status == ConnectionStatus.CONNECTED -> 22f
            else -> 16f
        },
        animationSpec = tween(220),
        label = "powerHaloShadowElevation"
    )

    val borderAlpha = when (status) {
        ConnectionStatus.CONNECTING -> (0.42f * pulseFactor).coerceAtMost(0.56f)
        ConnectionStatus.CONNECTED -> (0.34f * pulseFactor).coerceAtMost(0.42f)
        ConnectionStatus.DISCONNECTING -> (0.42f * pulseFactor).coerceAtMost(0.56f)
        ConnectionStatus.DISCONNECTED -> (0.24f * pulseFactor).coerceAtMost(0.30f)
    }
    val gradientAlpha = when (status) {
        ConnectionStatus.CONNECTING -> (0.36f * pulseFactor).coerceAtMost(0.48f)
        ConnectionStatus.CONNECTED -> (0.28f * pulseFactor).coerceAtMost(0.36f)
        ConnectionStatus.DISCONNECTING -> (0.36f * pulseFactor).coerceAtMost(0.48f)
        ConnectionStatus.DISCONNECTED -> (0.16f * pulseFactor).coerceAtMost(0.22f)
    }
    val haloShadowAlpha = when (status) {
        ConnectionStatus.CONNECTING -> (0.54f * pulseFactor).coerceAtMost(0.72f)
        ConnectionStatus.CONNECTED -> (0.44f * pulseFactor).coerceAtMost(0.56f)
        ConnectionStatus.DISCONNECTING -> (0.54f * pulseFactor).coerceAtMost(0.72f)
        ConnectionStatus.DISCONNECTED -> (0.22f * pulseFactor).coerceAtMost(0.30f)
    }
    val iconGlowAlpha = when (status) {
        ConnectionStatus.CONNECTING, ConnectionStatus.DISCONNECTING -> (0.88f * pulseFactor).coerceAtMost(0.98f)
        ConnectionStatus.CONNECTED -> (0.72f * pulseFactor).coerceAtMost(0.82f)
        ConnectionStatus.DISCONNECTED -> (0.48f * pulseFactor).coerceAtMost(0.56f)
    }

    val text = when (status) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.power_disconnect)
        ConnectionStatus.CONNECTING -> stringResource(R.string.power_connecting)
        ConnectionStatus.DISCONNECTING -> stringResource(R.string.power_turning_off)
        ConnectionStatus.DISCONNECTED -> stringResource(R.string.power_connect)
    }

    Box(
        modifier = Modifier
            .size(228.dp)
            .graphicsLayer {
                scaleX = containerScale
                scaleY = containerScale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(228.dp)
                .drawBehind {
                    val radius = size.minDimension / 2f
                    // outer soft halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = gradientAlpha),
                                accentColor.copy(alpha = gradientAlpha * 0.4f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.2f
                        )
                    )

                    // inner stronger glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = gradientAlpha * 0.4f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 0.85f
                        )
                    )
                }
                .shadow(
                    elevation = haloShadowElevation.dp,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = haloShadowAlpha * 0.72f),
                    spotColor = accentColor.copy(alpha = haloShadowAlpha)
                )
                .background(Color.Transparent, CircleShape)
        )
        Column(
            modifier = Modifier
                .size(200.dp)
                .shadow(
                    elevation = coreShadowElevation.dp,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = (0.24f * pulseFactor).coerceAtMost(0.42f)),
                    spotColor = accentColor.copy(alpha = (0.36f * pulseFactor).coerceAtMost(0.58f))
                )
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(
                    width = 1.5.dp,
                    color = accentColor.copy(alpha = borderAlpha),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    shape = CircleShape
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = actionDescription,
                    tint = accentColor.copy(alpha = iconGlowAlpha),
                    modifier = Modifier
                        .size(66.dp)
                        .graphicsLayer {
                            val scale = if (isTransitioning) iconBreathScale else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                )
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = actionDescription,
                    tint = accentColor,
                    modifier = Modifier
                        .size(62.dp)
                        .graphicsLayer {
                            val scale = if (isTransitioning) iconBreathScale else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
            Text(
                text = text,
                color = accentColor.copy(alpha = if (isTransitioning) 0.56f else 1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
