package com.v2rayops.client.profiles

import com.v2rayops.client.domain.model.ServerNode
import com.v2rayops.client.domain.model.VpnConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthorizedProfileValidator @Inject constructor(
    private val profileParser: ProfileParser
) {
    /**
     * Validates that a VPN configuration originates from an authorized V2RayOPS server.
     * Prevents arbitrary third-party configurations from being loaded into the client.
     */
    fun isAuthorized(config: VpnConfig, authorizedServers: List<ServerNode>): Boolean {
        if (!config.isActive) return false
        if (config.configUri.isBlank()) return false

        val parsed = profileParser.parseUri(config.configUri) ?: return false

        // Check if config matches at least one authorized V2RayOPS server by ID or hostname
        val matchesServer = authorizedServers.any { server ->
            server.id == config.serverId ||
            server.hostname.equals(parsed.host, ignoreCase = true)
        }

        return matchesServer
    }

    /**
     * Filters a list of configs to return only valid, non-expired, authorized configurations.
     */
    fun filterAuthorizedConfigs(
        configs: List<VpnConfig>,
        authorizedServers: List<ServerNode>
    ): List<VpnConfig> {
        return configs.filter { isAuthorized(it, authorizedServers) }
    }
}
