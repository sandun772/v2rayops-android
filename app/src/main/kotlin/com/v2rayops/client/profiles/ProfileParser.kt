package com.v2rayops.client.profiles

import com.v2rayops.client.core.logger.AppLogger
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedProfile(
    val protocol: String,
    val host: String,
    val port: Int,
    val uuidOrPassword: String,
    val remark: String,
    val security: String = "none",
    val sni: String = "",
    val flow: String = "",
    val networkType: String = "tcp",
    val wsPath: String = "",
    val wsHost: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val rawUri: String
)

@Singleton
class ProfileParser @Inject constructor() {

    fun parseUri(rawUri: String): ParsedProfile? {
        val trimmed = rawUri.trim()
        return try {
            when {
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
                else -> null
            }
        } catch (e: Exception) {
            AppLogger.d("ProfileParser", "Failed to parse URI: ${e.message}")
            null
        }
    }

    private fun parseVless(rawUri: String): ParsedProfile {
        val uri = URI(rawUri)
        val userInfo = uri.userInfo ?: ""
        val host = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val remark = uri.fragment?.let { safeDecode(it) } ?: "VLESS Config"

        val queryParams = parseQuery(uri.rawQuery)
        val security = queryParams["security"] ?: "none"
        val sni = queryParams["sni"] ?: ""
        val flow = queryParams["flow"] ?: ""
        val type = queryParams["type"] ?: "tcp"
        val path = queryParams["path"]?.let { safeDecode(it) } ?: ""
        val wsHost = queryParams["host"]?.let { safeDecode(it) } ?: host
        val pbk = queryParams["pbk"] ?: ""
        val sid = queryParams["sid"] ?: ""

        return ParsedProfile(
            protocol = "vless",
            host = host,
            port = port,
            uuidOrPassword = userInfo,
            remark = remark,
            security = security,
            sni = sni,
            flow = flow,
            networkType = type,
            wsPath = path,
            wsHost = wsHost,
            publicKey = pbk,
            shortId = sid,
            rawUri = rawUri
        )
    }

    private fun parseTrojan(rawUri: String): ParsedProfile {
        val uri = URI(rawUri)
        val password = uri.userInfo ?: ""
        val host = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val remark = uri.fragment?.let { safeDecode(it) } ?: "Trojan Config"

        val queryParams = parseQuery(uri.rawQuery)
        val security = queryParams["security"] ?: "tls"
        val sni = queryParams["sni"] ?: ""
        val type = queryParams["type"] ?: "tcp"
        val path = queryParams["path"]?.let { safeDecode(it) } ?: ""
        val wsHost = queryParams["host"]?.let { safeDecode(it) } ?: host

        return ParsedProfile(
            protocol = "trojan",
            host = host,
            port = port,
            uuidOrPassword = password,
            remark = remark,
            security = security,
            sni = sni,
            networkType = type,
            wsPath = path,
            wsHost = wsHost,
            rawUri = rawUri
        )
    }

    private fun parseVmess(rawUri: String): ParsedProfile? {
        val base64Content = rawUri.removePrefix("vmess://").trim()
        val jsonStr = String(Base64.getUrlDecoder().decode(base64Content), Charsets.UTF_8)
        val json = JSONObject(jsonStr)

        val host = json.optString("add")
        val port = json.optInt("port", 443)
        val uuid = json.optString("id")
        val remark = json.optString("ps", "VMess Config")
        val security = json.optString("tls", "none")
        val sni = json.optString("sni")
        val net = json.optString("net", "tcp")
        val path = json.optString("path", "")
        val wsHost = json.optString("host", host)

        if (host.isBlank() || uuid.isBlank()) return null

        return ParsedProfile(
            protocol = "vmess",
            host = host,
            port = port,
            uuidOrPassword = uuid,
            remark = remark,
            security = if (security == "tls") "tls" else "none",
            sni = sni,
            networkType = net,
            wsPath = path,
            wsHost = wsHost,
            rawUri = rawUri
        )
    }

    private fun parseShadowsocks(rawUri: String): ParsedProfile? {
        val uri = URI(rawUri)
        val host = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 8388
        val remark = uri.fragment?.let { safeDecode(it) } ?: "Shadowsocks Config"
        val userInfo = uri.userInfo ?: ""

        if (host.isBlank() || userInfo.isBlank()) return null

        return ParsedProfile(
            protocol = "shadowsocks",
            host = host,
            port = port,
            uuidOrPassword = userInfo,
            remark = remark,
            rawUri = rawUri
        )
    }

    private fun parseQuery(query: String?): Map<String, String> {
        if (query.isNullOrBlank()) return emptyMap()
        return query.split("&")
            .mapNotNull {
                val idx = it.indexOf('=')
                if (idx > 0) {
                    val key = it.substring(0, idx)
                    val value = it.substring(idx + 1)
                    key to value
                } else null
            }
            .toMap()
    }

    private fun safeDecode(text: String): String {
        return try {
            URLDecoder.decode(text, "UTF-8")
        } catch (_: Exception) {
            text
        }
    }
}
