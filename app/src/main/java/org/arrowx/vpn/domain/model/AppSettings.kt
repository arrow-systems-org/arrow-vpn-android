package org.arrowx.vpn.domain.model

data class AppSettings(
    val minimizeToTray: Boolean = true,
    val autoConnect: Boolean = false,
    val killSwitch: Boolean = false,
    val connectionMode: ConnectionMode = ConnectionMode.VPN
)
