package com.v2rayops.client.domain.model

data class VpnConfig(
    val id: Long,
    val protocol: String,
    val configUri: String,
    val serverId: Long,
    val status: String,
    val remark: String = "",
    val host: String = "",
    val port: Int = 443
) {
    val isVless: Boolean get() = protocol.equals("vless", ignoreCase = true)
    val isVmess: Boolean get() = protocol.equals("vmess", ignoreCase = true)
    val isTrojan: Boolean get() = protocol.equals("trojan", ignoreCase = true)
    val isShadowsocks: Boolean get() = protocol.equals("shadowsocks", ignoreCase = true)
    val isActive: Boolean get() = status.equals("active", ignoreCase = true)
}
