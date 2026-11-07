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
}
