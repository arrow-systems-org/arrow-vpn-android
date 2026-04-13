package org.arrowx.vpn.data.local

import android.content.Context
import org.arrowx.vpn.domain.model.AppSettings
import org.arrowx.vpn.domain.model.ConnectionMode
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.domain.model.inferCountryCode
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveCredentials(uuid: String, password: String) {
        prefs.edit()
            .putString(KEY_UUID, uuid)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_UUID)
            .remove(KEY_PASSWORD)
            .apply()
    }

    fun getUuid(): String = prefs.getString(KEY_UUID, "") ?: ""

    fun getPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""

    fun saveServers(servers: List<ServerNode>) {
        val array = JSONArray()
        servers.forEach { server ->
            val json = JSONObject()
            json.put("id", server.id)
            json.put("name", server.name)
            json.put("countryCode", server.countryCode)
            json.put("vlessConfig", server.vlessConfig)
            array.put(json)
        }
        prefs.edit().putString(KEY_SERVERS, array.toString()).apply()
    }

    fun getServers(): List<ServerNode> {
        val serversJson = prefs.getString(KEY_SERVERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(serversJson)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val id = json.optString("id")
                    val name = json.optString("name")
                    val countryCode = json.optString("countryCode").ifBlank {
                        inferCountryCode(
                            id = id,
                            name = name,
                            hint = json.optString("flagUrl")
                        )
                    }
                    add(
                        ServerNode(
                            id = id,
                            name = name,
                            countryCode = countryCode,
                            vlessConfig = json.optString("vlessConfig")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSelectedServerId(serverId: String) {
        prefs.edit().putString(KEY_SELECTED_SERVER, serverId).apply()
    }

    fun getSelectedServerId(): String? {
        return prefs.getString(KEY_SELECTED_SERVER, null)
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_TRAY, settings.minimizeToTray)
            .putBoolean(KEY_AUTO_CONNECT, settings.autoConnect)
            .putBoolean(KEY_KILL_SWITCH, settings.killSwitch)
            .putString(KEY_CONNECTION_MODE, settings.connectionMode.name)
            .apply()
    }

    fun getSettings(): AppSettings {
        val modeName = prefs.getString(KEY_CONNECTION_MODE, ConnectionMode.VPN.name)
        val mode = runCatching { ConnectionMode.valueOf(modeName.orEmpty()) }.getOrDefault(ConnectionMode.VPN)
        return AppSettings(
            minimizeToTray = prefs.getBoolean(KEY_TRAY, true),
            autoConnect = prefs.getBoolean(KEY_AUTO_CONNECT, false),
            killSwitch = prefs.getBoolean(KEY_KILL_SWITCH, false),
            connectionMode = mode
        )
    }

    companion object {
        private const val PREF_NAME = "arrowvpn_preferences"
        private const val KEY_UUID = "uuid"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SERVERS = "servers"
        private const val KEY_SELECTED_SERVER = "selected_server"
        private const val KEY_TRAY = "tray"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_KILL_SWITCH = "kill_switch"
        private const val KEY_CONNECTION_MODE = "connection_mode"
    }
}
