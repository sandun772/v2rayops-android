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
    val wsPath: String = "",
    // Per-config Optimization Mode & Kernel Presets
    val tuningMode: String? = null,           // "BALANCED", "SPEED", "STABILITY"
    val customMtu: Int? = null,               // e.g. 1380 (cellular), 1360 (stability), 1500
    val customTcpBufferKb: Int? = null,       // e.g. 1024 (speed), 512 (balanced), 256 (stability)
    val customDns: String? = null,            // e.g. "1.1.1.1"
    val dpiDelayFix: Boolean? = null,         // TLS fragmentation bypass
    val smartQoS: Boolean? = null,            // Adaptive QoS traffic shaping
    val measuredSpeedMbps: Int? = null,       // Live benchmarked peak download speed
    val measuredUploadMbps: Double? = null,   // Live benchmarked upload speed
    val measuredPingMs: Long? = null,         // Live benchmarked round-trip latency
    val stabilityScore: Int? = null           // Live benchmarked stability score (0-100)
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
        get() = host.contains("vpnxhub.com", ignoreCase = true) || host == "172.237.84.45" || host == "172.236.134.215"

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
            wsHost == "172.237.84.45" ||
            wsHost == "172.236.134.215" ||
            sni.contains("vpnxhub.com", ignoreCase = true) ||
            configUri.contains("vpnxhub.com", ignoreCase = true) ||
            configUri.contains("172.237.84.45") ||
            configUri.contains("172.236.134.215")
        )

    val effectiveAddr: String
        get() = if (host.isNotBlank()) "$host:$port" else "sg-v2rayops.vpnxhub.com:443"

    val effectiveSni: String
        get() = activeSni.ifBlank { host.ifBlank { "sg-v2rayops.vpnxhub.com" } }

    /**
     * Returns the actual user-facing bug host or SNI domain associated with this profile
     * (e.g. support.zoom.us, teams.microsoft.com, aka.ms) rather than the internal VPS domain.
     */
    val displayRouteDomain: String
        get() = if (isAddrFronting && host.isNotBlank()) host else effectiveSni
}
