package org.arrowx.vpn.domain.model

data class StartupData(
    val uuid: String,
    val password: String,
    val isLoggedIn: Boolean,
    val servers: List<ServerNode>,
    val selectedServerId: String?,
    val settings: AppSettings
)
