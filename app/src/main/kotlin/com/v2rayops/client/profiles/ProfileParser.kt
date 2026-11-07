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
        val remark = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "VLESS Server"

        val queryParams = parseQuery(uri.rawQuery)
        val security = queryParams["security"] ?: "none"
        val sni = queryParams["sni"] ?: ""
        val flow = queryParams["flow"] ?: ""
        val type = queryParams["type"] ?: "tcp"
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
        val remark = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "Trojan Server"

        val queryParams = parseQuery(uri.rawQuery)
        val security = queryParams["security"] ?: "tls"
        val sni = queryParams["sni"] ?: ""
        val type = queryParams["type"] ?: "tcp"

        return ParsedProfile(
            protocol = "trojan",
            host = host,
            port = port,
            uuidOrPassword = password,
            remark = remark,
            security = security,
            sni = sni,
            networkType = type,
            rawUri = rawUri
        )
    }

    private fun parseVmess(rawUri: String): ParsedProfile? {
        val base64Content = rawUri.removePrefix("vmess://").trim()
        val decodedBytes = try {
            Base64.getDecoder().decode(base64Content)
        } catch (_: Exception) {
            Base64.getUrlDecoder().decode(base64Content)
        }
        val decodedJson = String(decodedBytes, Charsets.UTF_8)
        val json = JSONObject(decodedJson)

        val host = json.optString("add", "")
        val port = json.optInt("port", 443)
        val id = json.optString("id", "")
        val remark = json.optString("ps", "VMess Server")
        val net = json.optString("net", "tcp")
        val tls = json.optString("tls", "none")
        val sni = json.optString("sni", "")

        return ParsedProfile(
            protocol = "vmess",
            host = host,
            port = port,
            uuidOrPassword = id,
            remark = remark,
            security = tls,
            sni = sni,
            networkType = net,
            rawUri = rawUri
        )
    }

    private fun parseShadowsocks(rawUri: String): ParsedProfile? {
        val uri = URI(rawUri)
        val host = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 8388
        val remark = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "Shadowsocks Server"
        val userInfo = uri.userInfo ?: ""

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
        return query.split("&").mapNotNull { param ->
            val parts = param.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val key = URLDecoder.decode(parts[0], "UTF-8")
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                key to value
            } else null
        }.toMap()
    }
}
