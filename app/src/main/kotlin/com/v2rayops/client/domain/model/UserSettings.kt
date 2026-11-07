package com.v2rayops.client.domain.model

enum class RoutingMode {
    GLOBAL,
    BYPASS_LAN_AND_SRI_LANKA,
    CUSTOM_RULES
}

data class UserSettings(
    val killSwitchEnabled: Boolean = false,
    val autoConnectOnWifi: Boolean = false,
    val autoConnectOnCellular: Boolean = false,
    val routingMode: RoutingMode = RoutingMode.GLOBAL,
    val customDns: String = "1.1.1.1",
    val perAppProxyEnabled: Boolean = false,
    val selectedServerId: Long? = null,
    val preferredProtocol: String = "vless"
)
