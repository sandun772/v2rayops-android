package com.v2rayops.client.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ProfileParserTest {

    private lateinit var parser: ProfileParser

    @Before
    fun setUp() {
        parser = ProfileParser()
    }

    @Test
    fun parseUri_withValidVlessReality_returnsParsedProfile() {
        // Sample standard VLESS Reality URI
        val uri = "vless://2ffb3ad4-50ab-1a0d-13ab-9f405ec5a45c@v2rayops.vpnxhub.com:443?security=reality&sni=v2rayops.vpnxhub.com&pbk=fakePub123&sid=1234&type=tcp#SG1-Singapore"

        val profile = parser.parseUri(uri)

        assertNotNull(profile)
        assertEquals("vless", profile?.protocol)
        assertEquals("v2rayops.vpnxhub.com", profile?.host)
        assertEquals(443, profile?.port)
        assertEquals("reality", profile?.security)
        assertEquals("SG1-Singapore", profile?.remark)
    }

    @Test
    fun parseUri_withUnsupportedScheme_returnsNull() {
        val uri = "http://unauthorized.com/config"
        val profile = parser.parseUri(uri)
        assertNull(profile)
    }

    @Test
    fun parseUri_withEmptyString_returnsNull() {
        val profile = parser.parseUri("")
        assertNull(profile)
    }

    @Test
    fun compareManualVsAutoFetched() {
        val manual = "vless://418d98af-55cf-4d90-be78-27e483d12c83@sg-v2rayops.vpnxhub.com:443?encryption=none&security=tls&sni=microsoft.com&allowInsecure=true&fp=chrome&type=ws&host=sg-v2rayops.vpnxhub.com&path=/v2rayops#Sandun%20Nimantha(267968)-Dialog-Zoom-V2RayOps%20(PAYG-Active)-Balanced"
        val auto = "vless://418d98af-55cf-4d90-be78-27e483d12c83@sg-v2rayops.vpnxhub.com:443?alpn=http%2F1.1%2Ch2%2Ch3&encryption=none&fp=chrome&host=sg-v2rayops.vpnxhub.com&path=%2Fv2rayops&security=tls&sni=microsoft.com&type=ws#Sandun%20Nimantha(267968)-Dialog-Zoom-V2RayOps%20(PAYG-Active)-Balanced"

        val pManual = parser.parseUri(manual)
        val pAuto = parser.parseUri(auto)

        assertNotNull(pManual)
        assertNotNull(pAuto)
        assertEquals(pManual?.uuidOrPassword, pAuto?.uuidOrPassword)
        assertEquals(pManual?.wsPath, pAuto?.wsPath)

        val generator = com.v2rayops.client.xray.XrayConfigGenerator(parser)
        val settings = com.v2rayops.client.domain.model.UserSettings()

        val cfgManual = com.v2rayops.client.domain.model.VpnConfig(
            id = 1, protocol = "vless", configUri = manual, serverId = 11, status = "active", configName = "Manual"
        )
        val cfgAuto = com.v2rayops.client.domain.model.VpnConfig(
            id = 2, protocol = "vless", configUri = auto, serverId = 11, status = "active", configName = "Auto"
        )

        val jsonManual = generator.generateJson(cfgManual, settings)
        val jsonAuto = generator.generateJson(cfgAuto, settings)

        val vcnManual = org.json.JSONObject(jsonManual).getJSONArray("outbounds").getJSONObject(0).getJSONObject("streamSettings").getJSONObject("tlsSettings").getString("verifyPeerCertByName")
        val vcnAuto = org.json.JSONObject(jsonAuto).getJSONArray("outbounds").getJSONObject(0).getJSONObject("streamSettings").getJSONObject("tlsSettings").getString("verifyPeerCertByName")

        assertEquals(vcnManual, vcnAuto)
        org.junit.Assert.assertTrue(vcnAuto.contains("*.vpnxhub.com"))
        org.junit.Assert.assertTrue(vcnAuto.contains("vpnxhub.com"))
        org.junit.Assert.assertTrue(pManual?.allowInsecure == true)
    }

    @Test
    fun parseUri_withAddrFrontingConfig_parsesCorrectly() {
        val uri = "vless://866b7211-8ff7-4c07-9713-eac6857486f0@support.zoom.us:443?encryption=none&security=tls&sni=sg-v2rayops.vpnxhub.com&allowInsecure=true&alpn=http/1.1,h2,h3&fp=chrome&type=ws&host=sg-v2rayops.vpnxhub.com&path=/v2rayops&packetEncoding=xudp#kaveen"
        val profile = parser.parseUri(uri)

        assertNotNull(profile)
        assertEquals("vless", profile?.protocol)
        assertEquals("support.zoom.us", profile?.host)
        assertEquals(443, profile?.port)
        assertEquals("866b7211-8ff7-4c07-9713-eac6857486f0", profile?.uuidOrPassword)
        assertEquals("sg-v2rayops.vpnxhub.com", profile?.sni)
        assertEquals("sg-v2rayops.vpnxhub.com", profile?.wsHost)
        assertEquals("/v2rayops", profile?.wsPath)
        assertEquals(true, profile?.allowInsecure)

        // Verify VpnConfig model behavior with this parsed profile
        val config = com.v2rayops.client.domain.model.VpnConfig(
            id = 100,
            protocol = profile!!.protocol,
            configUri = uri,
            serverId = 1,
            status = "active",
            host = profile.host,
            port = profile.port,
            sni = profile.sni,
            wsHost = profile.wsHost,
            wsPath = profile.wsPath
        )

        org.junit.Assert.assertTrue("Should identify as Addr-Fronting", config.isAddrFronting)
        assertEquals("support.zoom.us:443", config.effectiveAddr)
        assertEquals("sg-v2rayops.vpnxhub.com", config.effectiveSni)
    }

    @Test
    fun vpnConfig_withStandardSni_evaluatesAddrAndSniCleanly() {
        val uri = "vless://418d98af-55cf-4d90-be78-27e483d12c83@sg-v2rayops.vpnxhub.com:443?encryption=none&security=tls&sni=microsoft.com&allowInsecure=true&fp=chrome&type=ws&host=sg-v2rayops.vpnxhub.com&path=/v2rayops#Sandun"
        val profile = parser.parseUri(uri)
        assertNotNull(profile)

        val config = com.v2rayops.client.domain.model.VpnConfig(
            id = 101,
            protocol = profile!!.protocol,
            configUri = uri,
            serverId = 1,
            status = "active",
            host = profile.host,
            port = profile.port,
            sni = profile.sni,
            wsHost = profile.wsHost,
            wsPath = profile.wsPath
        )

        org.junit.Assert.assertFalse("Should NOT identify as Addr-Fronting", config.isAddrFronting)
        assertEquals("sg-v2rayops.vpnxhub.com:443", config.effectiveAddr)
        assertEquals("microsoft.com", config.effectiveSni)
    }
}
