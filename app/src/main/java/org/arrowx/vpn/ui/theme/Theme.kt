package org.arrowx.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArrowColorScheme = darkColorScheme(
    primary = ArrowAccentPurple,
    secondary = ArrowSuccess,
    tertiary = ArrowLinkBlue,
    background = ArrowBlack,
    surface = ArrowSurface,
    surfaceVariant = ArrowSurfaceSecondary,
    inverseSurface = ArrowSurfaceTertiary,
    outline = ArrowBorder,
    outlineVariant = ArrowBorder.copy(alpha = 0.55f),
    tertiaryContainer = ArrowWarning,
    onPrimary = ArrowPrimaryText,
    onSecondary = ArrowBlack,
    onTertiary = ArrowPrimaryText,
    onTertiaryContainer = ArrowPrimaryText,
    onBackground = ArrowPrimaryText,
    onSurface = ArrowPrimaryText,
    onSurfaceVariant = ArrowSecondaryText,
    inverseOnSurface = ArrowPrimaryText,
    error = ArrowError,
    onError = ArrowPrimaryText,
    scrim = Color.Black
)

@Composable
fun ArrowVpnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArrowColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
