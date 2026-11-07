package com.v2rayops.client.xray

import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.UserSettings
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.profiles.ProfileParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class XrayConfigGeneratorTest {

    private lateinit var generator: XrayConfigGenerator

    @Before
    fun setUp() {
        generator = XrayConfigGenerator(ProfileParser())
    }

    @Test
    fun generateJson_producesValidXrayStructure() {
        val config = VpnConfig(
            id = 10,
            protocol = "vless",
            configUri = "vless://2ffb3ad4-50ab-1a0d-13ab-9f405ec5a45c@v2rayops.vpnxhub.com:443?security=reality&sni=v2rayops.vpnxhub.com&pbk=fakePbk&sid=1234&type=tcp#SG1",
            serverId = 10,
            status = "active"
        )

        val settings = UserSettings(
            routingMode = RoutingMode.BYPASS_LAN_AND_SRI_LANKA,
            customDns = "1.1.1.1"
        )

        val jsonString = generator.generateJson(config, settings)
        assertNotNull(jsonString)

        val json = JSONObject(jsonString)

        // Inbounds validation (TUN + SOCKS + HTTP)
        val inbounds = json.getJSONArray("inbounds")
        assertEquals(3, inbounds.length())
        assertEquals("tun", inbounds.getJSONObject(0).getString("protocol"))
        assertEquals("tun-in", inbounds.getJSONObject(0).getString("tag"))
        assertEquals(10808, inbounds.getJSONObject(1).getInt("port"))
        assertEquals(10809, inbounds.getJSONObject(2).getInt("port"))

        // Outbounds validation
        val outbounds = json.getJSONArray("outbounds")
        assertTrue(outbounds.length() >= 3)
        assertEquals("proxy", outbounds.getJSONObject(0).getString("tag"))
        assertEquals("vless", outbounds.getJSONObject(0).getString("protocol"))

        // Routing rules
        val routing = json.getJSONObject("routing")
        val rules = routing.getJSONArray("rules")
        assertTrue(rules.length() >= 2)

        // Real-Time Stats & Policy validation (Phase 2)
        assertTrue(json.has("stats"))
        assertTrue(json.has("policy"))
        val policySystem = json.getJSONObject("policy").getJSONObject("system")
        assertTrue(policySystem.getBoolean("statsOutboundUplink"))
        assertTrue(policySystem.getBoolean("statsOutboundDownlink"))
    }

    @Test
    fun generateJson_withWebSocketVless_generatesWsSettings() {
        val wsConfig = VpnConfig(
            id = 141,
            protocol = "vless",
            configUri = "vless://8ad10bfa-00e3-49cb-b025-fa09b4053160@sg-v2rayops.vpnxhub.com:443?alpn=http%2F1.1%2Ch2%2Ch3&encryption=none&fp=chrome&host=sg-v2rayops.vpnxhub.com&path=%2Fv2rayops&security=tls&sni=tiktok.com&type=ws#TiktokPass",
            serverId = 11,
            status = "active",
            configName = "Dialog-Tiktok-Pass"
        )
        val settings = UserSettings()
        val json = JSONObject(generator.generateJson(wsConfig, settings))
        val proxy = json.getJSONArray("outbounds").getJSONObject(0)
        val stream = proxy.getJSONObject("streamSettings")
        assertEquals("ws", stream.getString("network"))
        assertEquals("tls", stream.getString("security"))
        val wsSettings = stream.getJSONObject("wsSettings")
        assertEquals("/v2rayops", wsSettings.getString("path"))
        assertEquals("sg-v2rayops.vpnxhub.com", wsSettings.getJSONObject("headers").getString("Host"))
    }
}
