package org.arrowx.vpn.data.repository

import org.arrowx.vpn.domain.model.AppSettings
import org.arrowx.vpn.domain.model.ConnectionMode
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.domain.model.StartupData

data class LoginOutcome(
    val isValid: Boolean,
    val message: String,
    val servers: List<ServerNode>
)

interface ArrowRepository {
    suspend fun preloadStartupData(): StartupData
    suspend fun loadCachedStartupData(): StartupData
    suspend fun login(uuid: String, password: String): LoginOutcome
    suspend fun measureLatencies(servers: List<ServerNode>): Map<String, Long?>
    fun loadSettings(): AppSettings
    fun saveSettings(settings: AppSettings)
    fun saveConnectionMode(mode: ConnectionMode)
    fun saveSelectedServer(serverId: String)
    fun logout()
    fun buildSubscriptionLink(uuid: String): String
}
