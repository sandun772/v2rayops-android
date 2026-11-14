package com.v2rayops.client.xray

import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.UserSettings
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.profiles.ProfileParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("tun", inbounds.getJSONObject(0).getString("tag"))
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
        val dnsRule = rules.getJSONObject(0)
        assertEquals("53", dnsRule.getString("port"))
        assertEquals("dns-out", dnsRule.getString("outboundTag"))

        // Real-Time Stats & Policy validation (Phase 2 & Smart QoS)
        assertTrue(json.has("stats"))
        assertTrue(json.has("policy"))
        val policy = json.getJSONObject("policy")
        val policySystem = policy.getJSONObject("system")
        assertTrue(policySystem.getBoolean("statsOutboundUplink"))
        assertTrue(policySystem.getBoolean("statsOutboundDownlink"))
        val policyLevels = policy.getJSONObject("levels")
        assertTrue(policyLevels.has("0"))
        assertTrue(policyLevels.has("8"))
        assertEquals(64, policyLevels.getJSONObject("8").getInt("bufferSize"))
        assertEquals(16, policyLevels.getJSONObject("0").getInt("bufferSize"))
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
        val jsonString = generator.generateJson(wsConfig, settings)
        println("GENERATED_WS_JSON:\n" + jsonString)
        val json = JSONObject(jsonString)
        val proxy = json.getJSONArray("outbounds").getJSONObject(0)
        val stream = proxy.getJSONObject("streamSettings")
        assertEquals("ws", stream.getString("network"))
        assertEquals("tls", stream.getString("security"))
        val wsSettings = stream.getJSONObject("wsSettings")
        assertEquals("/v2rayops", wsSettings.getString("path"))
        assertEquals("sg-v2rayops.vpnxhub.com", wsSettings.getString("host"))
        assertEquals("sg-v2rayops.vpnxhub.com", wsSettings.getJSONObject("headers").getString("Host"))
        val tlsSettings = stream.getJSONObject("tlsSettings")
        assertFalse(tlsSettings.has("allowInsecure"))
        assertTrue(tlsSettings.getString("verifyPeerCertByName").contains("sg-v2rayops.vpnxhub.com"))
        assertEquals("tiktok.com", tlsSettings.getString("serverName"))
        assertEquals("xudp", proxy.getJSONObject("settings").getString("packetEncoding"))
        assertTrue(json.getJSONObject("dns").has("hosts"))
    }

    @Test
    fun generateJson_withAddrFrontingVless_routesToClusterWithBugHostSni() {
        val addrFrontingConfig = VpnConfig(
            id = 142,
            protocol = "vless",
            configUri = "vless://866b7211-8ff7-4c07-9713-eac6857486f0@support.zoom.us:443?encryption=none&security=tls&sni=sg-v2rayops.vpnxhub.com&allowInsecure=true&alpn=http/1.1,h2,h3&fp=chrome&type=ws&host=sg-v2rayops.vpnxhub.com&path=/v2rayops&packetEncoding=xudp#kaveen%20Manahara",
            serverId = 11,
            status = "active",
            configName = "Dialog-Zoom-Fronting"
        )
        val settings = UserSettings()
        val jsonString = generator.generateJson(addrFrontingConfig, settings)
        val json = JSONObject(jsonString)

        val proxy = json.getJSONArray("outbounds").getJSONObject(0)
        val vnext = proxy.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0)

        // Socket routes to configured endpoint address
        assertEquals("support.zoom.us", vnext.getString("address"))
        assertEquals(443, vnext.getInt("port"))

        val stream = proxy.getJSONObject("streamSettings")
        val tlsSettings = stream.getJSONObject("tlsSettings")

        // SNI must preserve the configured SNI (sg-v2rayops.vpnxhub.com)
        assertEquals("sg-v2rayops.vpnxhub.com", tlsSettings.getString("serverName"))
        assertFalse(tlsSettings.has("allowInsecure"))
        assertTrue(tlsSettings.getString("verifyPeerCertByName").contains("sg-v2rayops.vpnxhub.com"))

        // WebSocket Host header must be our VPS domain
        val wsSettings = stream.getJSONObject("wsSettings")
        assertEquals("/v2rayops", wsSettings.getString("path"))
        assertEquals("sg-v2rayops.vpnxhub.com", wsSettings.getString("host"))
        assertEquals("sg-v2rayops.vpnxhub.com", wsSettings.getJSONObject("headers").getString("Host"))

        // DNS hosts mapping maps VPS cluster and maps known zoom edge
        val dnsHosts = json.getJSONObject("dns").getJSONObject("hosts")
        assertEquals("172.237.84.45", dnsHosts.getString("sg-v2rayops.vpnxhub.com"))
        assertTrue(dnsHosts.has("support.zoom.us"))

        // Routing rules must block UDP 443 to force graceful HTTP/2 fallback for media streaming
        val rules = json.getJSONObject("routing").getJSONArray("rules")
        var quicBlocked = false
        for (i in 0 until rules.length()) {
            val r = rules.getJSONObject(i)
            if (r.optString("port") == "443" && r.optString("network") == "udp" && r.optString("outboundTag") == "block") {
                quicBlocked = true
            }
        }
        assertTrue("QUIC UDP 443 must be blocked in routing rules", quicBlocked)
    }

    @Test
    fun generateJson_withGamingModeEnabled_clampsBufferTo16KbAndMtu1360() {
        val config = VpnConfig(
            id = 10,
            protocol = "vless",
            configUri = "vless://2ffb3ad4-50ab-1a0d-13ab-9f405ec5a45c@v2rayops.vpnxhub.com:443?security=reality&sni=v2rayops.vpnxhub.com&pbk=fakePbk&sid=1234&type=tcp#SG1",
            serverId = 10,
            status = "active"
        )

        val settings = UserSettings(
            gamingModeEnabled = true,
            tcpBufferSizeKb = 1024,
            dpiDelayFixEnabled = true
        )

        val jsonString = generator.generateJson(config, settings)
        val json = JSONObject(jsonString)

        // 1. Level 8 buffer size must be clamped strictly to 16KB for aggressive TCP backpressure
        val policyLevels = json.getJSONObject("policy").getJSONObject("levels")
        assertEquals(16, policyLevels.getJSONObject("8").getInt("bufferSize"))
        assertEquals(16, policyLevels.getJSONObject("0").getInt("bufferSize"))

        // 2. TUN inbound MTU must be 1360 to eliminate cellular carrier packet fragmentation
        val tunInbound = json.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("tun", tunInbound.getString("protocol"))
        assertEquals(1360, tunInbound.getJSONObject("settings").getInt("mtu"))

        // 3. DPI Delay Fix must be disabled in Gaming Mode to prevent TLS fragmentation pause
        val outbounds = json.getJSONArray("outbounds")
        var hasFragmentOutbound = false
        for (i in 0 until outbounds.length()) {
            if (outbounds.getJSONObject(i).optString("tag") == "fragment") {
                hasFragmentOutbound = true
            }
        }
        assertFalse("Fragment outbound must be absent in Gaming Mode", hasFragmentOutbound)

        // 4. Socket options must have keepalive 5s to prevent cellular tower radio sleep
        val proxyOutbound = outbounds.getJSONObject(0)
        val sockopt = proxyOutbound.getJSONObject("streamSettings").getJSONObject("sockopt")
        assertEquals(5, sockopt.getInt("tcpKeepAliveInterval"))
        assertTrue(sockopt.getBoolean("tcpNoDelay"))
    }
}
