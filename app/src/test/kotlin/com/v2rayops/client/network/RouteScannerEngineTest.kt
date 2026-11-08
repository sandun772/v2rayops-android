package com.v2rayops.client.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteScannerEngineTest {

    @Test
    fun defaultCandidates_containsBothStandardAndAddrFrontingStrategies() {
        val candidates = RouteScannerEngine.DEFAULT_CANDIDATES
        assertTrue("Candidates must not be empty", candidates.isNotEmpty())

        val addrSniList = candidates.filter { it.strategy == RouteStrategy.ADDR_SNI }
        val standardSniList = candidates.filter { it.strategy == RouteStrategy.STANDARD_SNI }

        assertEquals(addrSniList.size, standardSniList.size)

        // Verify ADDR_SNI fronting candidate targets the bug host domain directly with VPS cluster SNI
        val zoomAddrFronting = addrSniList.first { it.domain == "support.zoom.us" }
        assertEquals("support.zoom.us", zoomAddrFronting.targetHost)
        assertEquals(RouteScannerEngine.DEFAULT_VPS_DOMAIN, zoomAddrFronting.sni)
        assertEquals(RouteScannerEngine.DEFAULT_VPS_DOMAIN, zoomAddrFronting.wsHost)
        assertEquals("/v2rayops", zoomAddrFronting.wsPath)

        // Verify STANDARD_SNI candidate targets VPS IP directly with bug host SNI
        val zoomStandard = standardSniList.first { it.domain == "support.zoom.us" }
        assertEquals(RouteScannerEngine.DEFAULT_VPS_IP, zoomStandard.targetHost)
        assertEquals("support.zoom.us", zoomStandard.sni)
        assertEquals(RouteScannerEngine.DEFAULT_VPS_DOMAIN, zoomStandard.wsHost)
    }

    @Test
    fun generateCandidatesFromWords_expandsWordsIntoZeroRatedPatterns() {
        val keywords = listOf("zoom", "dialog", "tiktok")
        val generated = RouteScannerEngine.generateCandidatesFromWords(keywords)

        assertTrue(generated.isNotEmpty())

        // Must generate both ADDR_SNI and STANDARD_SNI for each expanded candidate
        val addrCandidates = generated.filter { it.strategy == RouteStrategy.ADDR_SNI }
        val sniCandidates = generated.filter { it.strategy == RouteStrategy.STANDARD_SNI }
        assertEquals(addrCandidates.size, sniCandidates.size)

        // Verify Zoom patterns exist
        val hasSupportZoom = generated.any { it.domain == "support.zoom.us" }
        val hasZoomUs = generated.any { it.domain == "zoom.us" }
        assertTrue("support.zoom.us must be generated", hasSupportZoom)
        assertTrue("zoom.us must be generated", hasZoomUs)

        // Verify Dialog patterns exist
        val hasDialogLk = generated.any { it.domain == "dialog.lk" }
        val hasPortalDialogLk = generated.any { it.domain == "portal.dialog.lk" }
        assertTrue("dialog.lk must be generated", hasDialogLk)
        assertTrue("portal.dialog.lk must be generated", hasPortalDialogLk)

        // Verify TikTok patterns exist
        val hasTiktokCdn = generated.any { it.domain == "tiktokcdn.com" || it.domain == "p16-va.tiktokcdn.com" }
        assertTrue("tiktok CDN domain must be generated", hasTiktokCdn)

        // For all ADDR_SNI candidates, targetHost must equal domain and SNI must equal VPS domain
        addrCandidates.forEach { candidate ->
            assertEquals(candidate.domain, candidate.targetHost)
            assertEquals(RouteScannerEngine.DEFAULT_VPS_DOMAIN, candidate.sni)
            assertEquals(RouteScannerEngine.DEFAULT_VPS_DOMAIN, candidate.wsHost)
        }
    }

    @Test
    fun generateCandidatesFromWords_handlesFullDomainsGracefully() {
        val input = listOf("support.zoom.us", "p16-va.tiktokcdn.com")
        val generated = RouteScannerEngine.generateCandidatesFromWords(input)

        val domains = generated.map { it.domain }.distinct()
        assertTrue(domains.contains("support.zoom.us"))
        assertTrue(domains.contains("p16-va.tiktokcdn.com"))
    }

    @Test
    fun commonProxyPorts_categorizesTlsAndHttpPortsCorrectly() {
        // Standard and Cloudflare TLS ports
        assertTrue(CommonProxyPorts.isTlsPort(443))
        assertTrue(CommonProxyPorts.isTlsPort(8443))
        assertTrue(CommonProxyPorts.isTlsPort(2053))
        assertTrue(CommonProxyPorts.isTlsPort(2083))
        assertTrue(CommonProxyPorts.isTlsPort(2087))
        assertTrue(CommonProxyPorts.isTlsPort(2096))

        // Standard and Cloudflare HTTP cleartext ports
        assertFalse(CommonProxyPorts.isTlsPort(80))
        assertFalse(CommonProxyPorts.isTlsPort(8080))
        assertFalse(CommonProxyPorts.isTlsPort(8880))
        assertFalse(CommonProxyPorts.isTlsPort(2052))
        assertFalse(CommonProxyPorts.isTlsPort(2082))
        assertFalse(CommonProxyPorts.isTlsPort(2086))
        assertFalse(CommonProxyPorts.isTlsPort(2095))

        // Quick multi ports must contain both TLS and HTTP ports
        assertTrue(CommonProxyPorts.QUICK_MULTI_PORTS.contains(443))
        assertTrue(CommonProxyPorts.QUICK_MULTI_PORTS.contains(8443))
        assertTrue(CommonProxyPorts.QUICK_MULTI_PORTS.contains(2083))
        assertTrue(CommonProxyPorts.QUICK_MULTI_PORTS.contains(80))
        assertTrue(CommonProxyPorts.QUICK_MULTI_PORTS.contains(8080))
    }

    @Test
    fun portScanResult_formatsPortNamesCorrectly() {
        val r443 = PortScanResult(443, PortProtocol.TLS, isOpen = true)
        val r8443 = PortScanResult(8443, PortProtocol.TLS, isOpen = true)
        val r80 = PortScanResult(80, PortProtocol.HTTP, isOpen = true)
        val r8080 = PortScanResult(8080, PortProtocol.HTTP, isOpen = true)

        assertTrue(r443.portName.contains("443"))
        assertTrue(r8443.portName.contains("8443"))
        assertTrue(r80.portName.contains("80"))
        assertTrue(r8080.portName.contains("8080"))
    }

    @Test
    fun threeXUiInboundSpec_generatesAccurateSpecs() {
        val tlsSpec = ThreeXUiInboundSpec(
            remark = "V2RayOps-VLESS-WS-8443",
            protocol = "vless",
            port = 8443,
            isTls = true,
            security = "tls",
            listen = "0.0.0.0",
            network = "ws",
            wsPath = "/v2rayops",
            wsHost = "sg-v2rayops.vpnxhub.com",
            sni = "sg-v2rayops.vpnxhub.com",
            clientUuid = "12345678-1234-1234-1234-123456789abc",
            panelUrl = "https://v2rayops.vpnxhub.com:2083/",
            fullJsonSnippet = "{\"port\": 8443, \"protocol\": \"vless\"}",
            instructions = listOf("Step 1: Open 3X-UI")
        )

        assertEquals(8443, tlsSpec.port)
        assertTrue(tlsSpec.isTls)
        assertEquals("tls", tlsSpec.security)
        assertTrue(tlsSpec.summaryText.contains("Port: 8443"))
        assertTrue(tlsSpec.summaryText.contains("Protocol: vless"))
        assertTrue(tlsSpec.summaryText.contains("Network (Transmission): ws"))
        assertTrue(tlsSpec.summaryText.contains("Security: tls"))

        val httpSpec = ThreeXUiInboundSpec(
            remark = "V2RayOps-VLESS-WS-80",
            protocol = "vless",
            port = 80,
            isTls = false,
            security = "none",
            listen = "0.0.0.0",
            network = "ws",
            wsPath = "/v2rayops",
            wsHost = "sg-v2rayops.vpnxhub.com",
            sni = "",
            clientUuid = "12345678-1234-1234-1234-123456789abc",
            panelUrl = "https://v2rayops.vpnxhub.com:2083/",
            fullJsonSnippet = "{\"port\": 80, \"protocol\": \"vless\"}",
            instructions = listOf("Step 1: Open 3X-UI")
        )

        assertEquals(80, httpSpec.port)
        assertFalse(httpSpec.isTls)
        assertEquals("none", httpSpec.security)
        assertTrue(httpSpec.summaryText.contains("Security: none"))
    }

    @Test
    fun candidate_supportsCustomTargetPortAndVerifiedPorts() {
        val base = RouteScannerEngine.DEFAULT_CANDIDATES.first()
        assertEquals(443, base.targetPort)

        val updatedPort = base.copy(
            targetPort = 8443,
            verifiedPorts = listOf(443, 8443, 2083, 80)
        )

        assertEquals(8443, updatedPort.targetPort)
        assertEquals(4, updatedPort.verifiedPorts.size)
        assertTrue(updatedPort.verifiedPorts.contains(80))
        assertTrue(updatedPort.verifiedPorts.contains(8443))
    }
}
