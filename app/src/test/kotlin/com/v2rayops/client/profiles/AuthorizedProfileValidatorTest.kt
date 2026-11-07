package com.v2rayops.client.profiles

import com.v2rayops.client.domain.model.ServerNode
import com.v2rayops.client.domain.model.VpnConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthorizedProfileValidatorTest {

    private lateinit var validator: AuthorizedProfileValidator
    private val authorizedServers = listOf(
        ServerNode(id = 10, name = "SG1", hostname = "v2rayops.vpnxhub.com", port = 443)
    )

    @Before
    fun setUp() {
        validator = AuthorizedProfileValidator(ProfileParser())
    }

    @Test
    fun isAuthorized_withAuthorizedServerHost_returnsTrue() {
        val config = VpnConfig(
            id = 1,
            protocol = "vless",
            configUri = "vless://uuid@v2rayops.vpnxhub.com:443?security=reality#SG1",
            serverId = 10,
            status = "active"
        )

        val result = validator.isAuthorized(config, authorizedServers)
        assertTrue(result)
    }

    @Test
    fun isAuthorized_withThirdPartyHost_returnsFalse() {
        val rogueConfig = VpnConfig(
            id = 2,
            protocol = "vless",
            configUri = "vless://uuid@rogue-vpn.attacker.com:443#RogueNode",
            serverId = 999,
            status = "active"
        )

        val result = validator.isAuthorized(rogueConfig, authorizedServers)
        assertFalse(result)
    }

    @Test
    fun isAuthorized_withInactiveConfig_returnsFalse() {
        val inactiveConfig = VpnConfig(
            id = 3,
            protocol = "vless",
            configUri = "vless://uuid@v2rayops.vpnxhub.com:443#SG1",
            serverId = 10,
            status = "disabled"
        )

        val result = validator.isAuthorized(inactiveConfig, authorizedServers)
        assertFalse(result)
    }
}
