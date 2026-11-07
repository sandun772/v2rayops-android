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
    }
}
