package org.arrowx.vpn.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.arrowx.vpn.domain.model.ServerNode
import org.arrowx.vpn.domain.model.inferCountryCode
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URL
import kotlin.system.measureTimeMillis

data class LoginApiResult(
    val isValid: Boolean,
    val message: String,
    val servers: List<ServerNode>
)

class ArrowApiClient {
    suspend fun login(
        uuid: String,
        password: String,
        connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS
    ): LoginApiResult = withContext(Dispatchers.IO) {
        val connection = (URL("$BASE_URL/api/login").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; utf-8")
            doOutput = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
        }

        val payload = JSONObject().apply {
            put("uuid", uuid)
            put("password", password)
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val body = runCatching {
            connection.inputStream.bufferedReader().use { it.readText() }
        }.getOrElse {
            connection.errorStream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
        }

        val json = runCatching { JSONObject(body) }.getOrElse { JSONObject() }
        val isValid = connection.responseCode == HttpURLConnection.HTTP_OK && json.optBoolean("valido", false)
        val message = json.optString("msg").ifBlank {
            if (isValid) "Acceso verificado" else "Credenciales incorrectas"
        }
        val servers = if (isValid) parseServers(json.optJSONObject("servidores")) else emptyList()
        connection.disconnect()
        LoginApiResult(
            isValid = isValid,
            message = message,
            servers = servers
        )
    }

    suspend fun measureServerLatency(server: ServerNode): Long? = withContext(Dispatchers.IO) {
        val uri = runCatching { URI(server.vlessConfig) }.getOrNull() ?: return@withContext null
        val host = uri.host ?: return@withContext null
        val port = if (uri.port == -1) 443 else uri.port
        runCatching {
            measureTimeMillis {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 2_000)
                }
            }
        }.getOrNull()
    }

    private fun parseServers(serversJson: JSONObject?): List<ServerNode> {
        if (serversJson == null) return emptyList()
        val keys = serversJson.keys()
        val servers = mutableListOf<ServerNode>()
        while (keys.hasNext()) {
            val key = keys.next()
            val node = serversJson.optJSONObject(key) ?: continue
            val vless = node.optString("vless")
            if (vless.isBlank()) continue
            val name = node.optString("nombre", key.uppercase())
            val countryHint = node.optString("codigo_pais")
                .ifBlank { node.optString("country_code") }
                .ifBlank { node.optString("pais") }
                .ifBlank { node.optString("bandera") }
            servers.add(
                ServerNode(
                    id = key,
                    name = name,
                    countryCode = inferCountryCode(
                        id = key,
                        name = name,
                        hint = countryHint
                    ),
                    vlessConfig = vless
                )
            )
        }
        return servers.sortedBy { it.name }
    }

    companion object {
        private const val BASE_URL = "https://arrow-x.org:5000"
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000
        private const val DEFAULT_READ_TIMEOUT_MS = 10_000
    }
}
