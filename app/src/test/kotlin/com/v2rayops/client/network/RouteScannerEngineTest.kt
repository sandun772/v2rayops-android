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
}
