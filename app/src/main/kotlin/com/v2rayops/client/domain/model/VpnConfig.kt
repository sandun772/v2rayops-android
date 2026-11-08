package com.v2rayops.client.domain.model

data class VpnConfig(
    val id: Long,
    val protocol: String,
    val configUri: String,
    val serverId: Long,
    val status: String,
    val configName: String = "",
    val remark: String = "",
    val host: String = "",
    val port: Int = 443,
    val expiresAt: String? = null,
    val serverName: String = "",
    val serverCountry: String = "",
    val serverFlag: String = "",
    val sni: String = "",
    val wsHost: String = "",
    val wsPath: String = ""
) {
    val isVless: Boolean get() = protocol.equals("vless", ignoreCase = true)
    val isVmess: Boolean get() = protocol.equals("vmess", ignoreCase = true)
    val isTrojan: Boolean get() = protocol.equals("trojan", ignoreCase = true)
    val isShadowsocks: Boolean get() = protocol.equals("shadowsocks", ignoreCase = true)
    val isActive: Boolean get() = status.equals("active", ignoreCase = true)

    val displayName: String
        get() = configName.ifBlank { remark.ifBlank { "Config #$id ($protocol)" } }

    val displayEndpoint: String
        get() = if (host.isNotBlank()) "$host:$port" else "v2rayops.vpnxhub.com:443"

    val isVpsHost: Boolean
        get() = host.contains("vpnxhub.com", ignoreCase = true) || host == "172.236.134.215"

    val activeSni: String
        get() {
            if (sni.isNotBlank()) return sni
            val regex = Regex("""[?&]sni=([^&#]*)""")
            val match = regex.find(configUri)
            val sniParam = match?.groupValues?.get(1)?.let {
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
            } ?: ""
            return if (sniParam.isNotBlank()) sniParam else host
        }

    val isAddrFronting: Boolean
        get() = !isVpsHost && (
            wsHost.contains("vpnxhub.com", ignoreCase = true) ||
            wsHost == "172.236.134.215" ||
            sni.contains("vpnxhub.com", ignoreCase = true) ||
            configUri.contains("vpnxhub.com", ignoreCase = true) ||
            configUri.contains("172.236.134.215")
        )

    val effectiveAddr: String
        get() = if (host.isNotBlank()) "$host:$port" else "sg-v2rayops.vpnxhub.com:443"

    val effectiveSni: String
        get() = activeSni.ifBlank { host.ifBlank { "sg-v2rayops.vpnxhub.com" } }
}
