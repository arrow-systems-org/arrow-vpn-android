package org.arrowx.vpn.data.repository

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.arrowx.vpn.R
import org.arrowx.vpn.data.local.AppPreferences
import org.arrowx.vpn.data.remote.ArrowApiClient
import org.arrowx.vpn.domain.model.AppSettings
import org.arrowx.vpn.domain.model.ConnectionMode
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.domain.model.StartupData
import java.util.Locale

class ArrowRepositoryImpl(
    private val context: Context,
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
                val syncedServers = localizeServers(loginAttempt.servers.ifEmpty { cachedData.servers })
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
        val servers = localizeServers(cachedServersDeferred.await().ifEmpty { defaultServers() })

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
        val localizedServers = localizeServers(result.servers)

        if (result.isValid) {
            preferences.saveCredentials(uuid, password)
            if (localizedServers.isNotEmpty()) {
                preferences.saveServers(localizedServers)
                preferences.saveSelectedServerId(localizedServers.first().id)
            }
        }

        LoginOutcome(
            isValid = result.isValid,
            message = result.message,
            servers = localizedServers
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

    private fun localizeServers(servers: List<ServerNode>): List<ServerNode> {
        if (servers.isEmpty()) return emptyList()
        val locale = appLocale()
        return servers
            .map { server -> server.copy(name = localizeServerName(server, locale)) }
            .sortedBy { it.name }
    }

    private fun localizeServerName(server: ServerNode, locale: Locale): String {
        val normalizedCountryCode = server.countryCode.trim().uppercase(Locale.ROOT)
        if (normalizedCountryCode.length == 2 &&
            normalizedCountryCode.all { it.isLetter() } &&
            normalizedCountryCode != "UN"
        ) {
            val localizedCountryName = Locale.Builder()
                .setRegion(normalizedCountryCode)
                .build()
                .getDisplayCountry(locale)
            if (localizedCountryName.isNotBlank() &&
                !localizedCountryName.equals(normalizedCountryCode, ignoreCase = true)
            ) {
                return localizedCountryName
            }
        }

        return when (server.id.lowercase(Locale.ROOT)) {
            "germany" -> context.getString(R.string.de_server_node)
            "austria" -> context.getString(R.string.at_server_node)
            "sweden" -> context.getString(R.string.se_server_node)
            "switzerland" -> context.getString(R.string.ch_server_node)
            "usa", "us", "united_states", "unitedstates" -> context.getString(R.string.us_server_node)
            else -> server.name
        }
    }

    @Suppress("DEPRECATION")
    private fun appLocale(): Locale {
        val configuration = context.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !configuration.locales.isEmpty) {
            return configuration.locales[0]
        }
        return configuration.locale
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
                name = context.getString(R.string.de_server_node),
                countryCode = "DE",
                vlessConfig = "vless://config-alemania-placeholder"
            ),
            ServerNode(
                id = "austria",
                name = context.getString(R.string.at_server_node),
                countryCode = "AT",
                vlessConfig = "vless://config-austria-placeholder"
            ),
            ServerNode(
                id = "sweden",
                name = context.getString(R.string.se_server_node),
                countryCode = "SE",
                vlessConfig = "vless://config-suecia-placeholder"
            ),
            ServerNode(
                id = "switzerland",
                name = context.getString(R.string.ch_server_node),
                countryCode = "CH",
                vlessConfig = "vless://config-suiza-placeholder"
            ),
            ServerNode(
                id = "usa",
                name = context.getString(R.string.us_server_node),
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
