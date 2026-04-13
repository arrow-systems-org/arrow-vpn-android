package org.arrowx.vpn.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.arrowx.vpn.data.local.AppPreferences
import org.arrowx.vpn.data.remote.ArrowApiClient
import org.arrowx.vpn.domain.model.AppSettings
import org.arrowx.vpn.domain.model.ConnectionMode
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.domain.model.StartupData

class ArrowRepositoryImpl(
    private val apiClient: ArrowApiClient,
    private val preferences: AppPreferences
) : ArrowRepository {

    override suspend fun preloadStartupData(): StartupData = coroutineScope {
        val cachedData = loadCachedStartupData()
        if (cachedData.uuid.isBlank() || cachedData.password.isBlank()) return@coroutineScope cachedData.copy(isLoggedIn = false)

        val loginAttempt = runCatching {
            apiClient.login(
                uuid = cachedData.uuid,
                password = cachedData.password,
                connectTimeoutMs = STARTUP_CONNECT_TIMEOUT_MS,
                readTimeoutMs = STARTUP_READ_TIMEOUT_MS
            )
        }.getOrNull()

        when {
            loginAttempt == null -> cachedData
            loginAttempt.isValid -> {
                val syncedServers = loginAttempt.servers.ifEmpty { cachedData.servers }
                if (loginAttempt.servers.isNotEmpty()) {
                    preferences.saveServers(syncedServers)
                }
                val selectedServerId = resolveSelectedServer(
                    preferredServerId = cachedData.selectedServerId,
                    servers = syncedServers
                )
                if (!selectedServerId.isNullOrBlank()) {
                    preferences.saveSelectedServerId(selectedServerId)
                }
                cachedData.copy(
                    isLoggedIn = true,
                    servers = syncedServers,
                    selectedServerId = selectedServerId
                )
            }

            else -> cachedData.copy(isLoggedIn = false)
        }
    }

    override suspend fun loadCachedStartupData(): StartupData = coroutineScope {
        val uuidDeferred = async(Dispatchers.IO) { preferences.getUuid() }
        val passwordDeferred = async(Dispatchers.IO) { preferences.getPassword() }
        val settingsDeferred = async(Dispatchers.IO) { preferences.getSettings() }
        val cachedServersDeferred = async(Dispatchers.IO) { preferences.getServers() }
        val selectedServerDeferred = async(Dispatchers.IO) { preferences.getSelectedServerId() }

        val uuid = uuidDeferred.await()
        val password = passwordDeferred.await()
        val settings = settingsDeferred.await()
        val servers = cachedServersDeferred.await().ifEmpty { defaultServers() }

        val selectedServerId = resolveSelectedServer(
            preferredServerId = selectedServerDeferred.await(),
            servers = servers
        )

        StartupData(
            uuid = uuid,
            password = password,
            isLoggedIn = uuid.isNotBlank() && password.isNotBlank(),
            servers = servers,
            selectedServerId = selectedServerId,
            settings = settings
        )
    }

    override suspend fun login(uuid: String, password: String): LoginOutcome = withContext(Dispatchers.IO) {
        val result = runCatching { apiClient.login(uuid, password) }.getOrElse {
            return@withContext LoginOutcome(
                isValid = false,
                message = "Error de conexión con el servidor maestro.",
                servers = emptyList()
            )
        }

        if (result.isValid) {
            preferences.saveCredentials(uuid, password)
            if (result.servers.isNotEmpty()) {
                preferences.saveServers(result.servers)
                preferences.saveSelectedServerId(result.servers.first().id)
            }
        }

        LoginOutcome(
            isValid = result.isValid,
            message = result.message,
            servers = result.servers
        )
    }

    override suspend fun measureLatencies(servers: List<ServerNode>): Map<String, Long?> = coroutineScope {
        servers.map { server ->
            async(Dispatchers.IO) {
                server.id to apiClient.measureServerLatency(server)
            }
        }.awaitAll().toMap()
    }

    override fun loadSettings(): AppSettings = preferences.getSettings()

    override fun saveSettings(settings: AppSettings) {
        preferences.saveSettings(settings)
    }

    override fun saveConnectionMode(mode: ConnectionMode) {
        val updated = loadSettings().copy(connectionMode = mode)
        saveSettings(updated)
    }

    override fun saveSelectedServer(serverId: String) {
        preferences.saveSelectedServerId(serverId)
    }

    override fun logout() {
        preferences.clearCredentials()
    }

    override fun buildSubscriptionLink(uuid: String): String {
        return "https://arrow-x.org:5000/sub/$uuid"
    }
    private fun resolveSelectedServer(
        preferredServerId: String?,
        servers: List<ServerNode>
    ): String? {
        return preferredServerId
            ?.takeIf { chosenId -> servers.any { it.id == chosenId } }
            ?: servers.firstOrNull()?.id
    }

    private fun defaultServers(): List<ServerNode> {
        return listOf(
            ServerNode(
                id = "germany",
                name = "Alemania",
                countryCode = "DE",
                vlessConfig = "vless://config-alemania-placeholder"
            ),
            ServerNode(
                id = "austria",
                name = "Austria",
                countryCode = "AT",
                vlessConfig = "vless://config-austria-placeholder"
            ),
            ServerNode(
                id = "sweden",
                name = "Suecia",
                countryCode = "SE",
                vlessConfig = "vless://config-suecia-placeholder"
            ),
            ServerNode(
                id = "switzerland",
                name = "Suiza",
                countryCode = "CH",
                vlessConfig = "vless://config-suiza-placeholder"
            ),
            ServerNode(
                id = "usa",
                name = "USA",
                countryCode = "US",
                vlessConfig = "vless://config-usa-placeholder"
            )
        )
    }

    companion object {
        private const val STARTUP_CONNECT_TIMEOUT_MS = 1_500
        private const val STARTUP_READ_TIMEOUT_MS = 1_500
    }
}
