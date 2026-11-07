package com.v2rayops.client.domain.model

data class ServerNode(
    val id: Long,
    val name: String,
    val hostname: String,
    val port: Int = 443,
    val latencyMs: Long? = null,
    val isOnline: Boolean = true
) {
    val displayName: String get() = name.ifBlank { hostname }
    val displayEndpoint: String get() = "$hostname:$port"
}
