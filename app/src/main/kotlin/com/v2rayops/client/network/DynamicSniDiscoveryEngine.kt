package com.v2rayops.client.network

import android.content.Context
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.domain.model.VpnConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Dynamic SNI Discovery Engine:
 * Dynamically discovers, expands, and resolves all zero-rated bug hosts and edge fronting
 * SNIs corresponding to the client's configuration package (e.g. Zoom, Work & Learn, Teams, TikTok, Social).
 *
 * Capabilities:
 * 1. Ecosystem Multi-Domain Catalog: Automatically expands package keywords into comprehensive cloud suites
 *    (e.g. Zoom package zero-rates Zoom + Microsoft Teams + Office 365 + Azure CDN + aka.ms + Edu domains).
 * 2. Dynamic TLS SAN (Subject Alternative Names) Harvesting: Queries live TLS handshakes to extract
 *    valid SAN DNS names from certificate chains in real-time.
 * 3. Physical Network DNS Verification: Resolves candidates to verified IPv4 endpoints before inclusion.
 * 4. Dual Strategy Candidate Generation: Emits paired ADDR_SNI and STANDARD_SNI candidates for every target.
 */
@Singleton
class DynamicSniDiscoveryEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routeScannerEngine: RouteScannerEngine,
    private val dispatchers: AppDispatchers
) {
    companion object {
        private const val TAG = "DynamicSniDiscovery"

        // Comprehensive Work & Learn / Zoom / Teams / Education zero-rated ecosystem
        private val WORK_AND_LEARN_ECOSYSTEM = listOf(
            // Zoom Core & CDN
            "zoom.us",
            "support.zoom.us",
            "us04web.zoom.us",
            "us05web.zoom.us",
            "marketplace.zoom.us",
            "zoomgov.com",
            "st1.zoom.us",
            "st2.zoom.us",

            // Microsoft, Teams & Office 365 (Widely zero-rated under Work & Learn)
            "microsoft.com",
            "teams.microsoft.com",
            "aka.ms",
            "office.com",
            "login.microsoftonline.com",
            "portal.azure.com",
            "azureedge.net",
            "sharepoint.com",
            "onedrive.live.com",
            "msftauth.net",
            "teams.live.com",
            "skype.com",

            // Google Meet & Workspace
            "meet.google.com",
            "classroom.google.com",
            "drive.google.com",

            // Sri Lanka National Education & Universities
            "learn.ac.lk",
            "ac.lk",
            "moe.gov.lk",
            "nie.lk"
        )

        // TikTok Zero-Rated CDN Ecosystem
        private val TIKTOK_ECOSYSTEM = listOf(
            "tiktok.com",
            "tiktokv.com",
            "byteoversea.com",
            "ibyteimg.com",
            "musical.ly",
            "tiktokcdn.com",
            "p16-va.tiktokcdn.com",
            "v16m.tiktokcdn.com",
            "api.tiktokv.com"
        )

        // WhatsApp & Meta Social Ecosystem
        private val WHATSAPP_SOCIAL_ECOSYSTEM = listOf(
            "whatsapp.com",
            "whatsapp.net",
            "static.whatsapp.net",
            "media.whatsapp.net",
            "web.whatsapp.com",
            "facebook.com",
            "fbcdn.net",
            "instagram.com",
            "cdninstagram.com"
        )

        // YouTube & Google Video Ecosystem
        private val YOUTUBE_ECOSYSTEM = listOf(
            "youtube.com",
            "googlevideo.com",
            "youtu.be",
            "ytimg.com",
            "i.ytimg.com",
            "yt3.ggpht.com"
        )

        // Gaming Zero-Rated Ecosystem
        private val GAMING_ECOSYSTEM = listOf(
            "freefiremobile.com",
            "garena.com",
            "steampowered.com",
            "steamcontent.com",
            "riotgames.com"
        )

        // Universal Edge & Anycast CDN endpoints
        private val UNIVERSAL_EDGE_ECOSYSTEM = listOf(
            "cloudflare.com",
            "one.one.one.one",
            "speed.cloudflare.com",
            "cdnjs.cloudflare.com"
        )
    }

    /**
     * Discovers all available candidate SNIs for the given configuration package.
     * Combines ecosystem catalog, config remarks, DNS pre-flight verification,
     * and live TLS SAN certificate extraction.
     */
    suspend fun discoverCandidates(
        config: VpnConfig?,
        onLog: (String) -> Unit = {}
    ): List<SniRouteCandidate> = withContext(dispatchers.io) {
        val rawPackageText = buildString {
            if (config != null) {
                append(config.displayName.lowercase(Locale.ROOT))
                append(" ")
                append(config.remark.lowercase(Locale.ROOT))
                append(" ")
                append(config.serverName.lowercase(Locale.ROOT))
            }
        }

        onLog("Analyzing configuration profile package: '$rawPackageText'...")
        val seedDomains = mutableSetOf<String>()

        // 1. Expand ecosystem based on detected package keywords
        val isWorkLearn = "zoom" in rawPackageText || "teams" in rawPackageText ||
                "work" in rawPackageText || "learn" in rawPackageText ||
                "dialog" in rawPackageText || "mobitel" in rawPackageText ||
                "office" in rawPackageText || "meet" in rawPackageText

        if (isWorkLearn) {
            onLog("Matched Work & Learn package: expanding Zoom + Microsoft Teams + Office 365 + Edu catalog (${WORK_AND_LEARN_ECOSYSTEM.size} endpoints)")
            seedDomains.addAll(WORK_AND_LEARN_ECOSYSTEM)
        }

        if ("tiktok" in rawPackageText) {
            onLog("Matched TikTok package: expanding ByteDance CDN catalog (${TIKTOK_ECOSYSTEM.size} endpoints)")
            seedDomains.addAll(TIKTOK_ECOSYSTEM)
        }

        if ("whatsapp" in rawPackageText || "chat" in rawPackageText || "social" in rawPackageText) {
            onLog("Matched Social package: expanding WhatsApp/Meta catalog (${WHATSAPP_SOCIAL_ECOSYSTEM.size} endpoints)")
            seedDomains.addAll(WHATSAPP_SOCIAL_ECOSYSTEM)
        }

        if ("youtube" in rawPackageText || "video" in rawPackageText || "stream" in rawPackageText) {
            onLog("Matched Video package: expanding YouTube/Google CDN catalog (${YOUTUBE_ECOSYSTEM.size} endpoints)")
            seedDomains.addAll(YOUTUBE_ECOSYSTEM)
        }

        if ("game" in rawPackageText || "fire" in rawPackageText || "pubg" in rawPackageText) {
            onLog("Matched Gaming package: expanding Game CDN catalog (${GAMING_ECOSYSTEM.size} endpoints)")
            seedDomains.addAll(GAMING_ECOSYSTEM)
        }

        // Always include universal Anycast edge endpoints as high-speed fallbacks
        seedDomains.addAll(UNIVERSAL_EDGE_ECOSYSTEM)

        // Include current config's host/sni if customized
        if (config != null && config.host.isNotBlank() && !config.host.contains("vpnxhub.com", ignoreCase = true)) {
            seedDomains.add(config.host)
        }
        if (config != null && config.effectiveSni.isNotBlank() && !config.effectiveSni.contains("vpnxhub.com", ignoreCase = true)) {
            seedDomains.add(config.effectiveSni)
        }

        // 2. Dynamic TLS SAN Discovery from core root seeds (e.g. microsoft.com, zoom.us)
        val rootsToHarvest = seedDomains.filter { it in listOf("microsoft.com", "zoom.us", "office.com", "tiktok.com") }
        if (rootsToHarvest.isNotEmpty()) {
            onLog("Harvesting live TLS Certificate Subject Alternative Names (SANs) from core seeds...")
            for (root in rootsToHarvest) {
                try {
                    val harvestedSans = harvestSanFromHost(root)
                    if (harvestedSans.isNotEmpty()) {
                        val validSans = harvestedSans
                            .filter { !it.contains("*") && it.length in 5..50 && !it.contains("/") }
                            .take(4)
                        if (validSans.isNotEmpty()) {
                            onLog("Discovered live SAN endpoints from $root: ${validSans.joinToString(", ")}")
                            seedDomains.addAll(validSans)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.d(TAG, "SAN harvesting skipped for $root: ${e.message}")
                }
            }
        }

        // 3. Fast Parallel Physical DNS Resolution Filter
        onLog("Verifying pure network DNS reachability for ${seedDomains.size} candidate endpoints...")
        val physicalNet = routeScannerEngine.getUnderlyingPhysicalNetwork()
        val verifiedDomains = seedDomains.map { domain ->
            async(dispatchers.io) {
                val hasIpv4 = try {
                    val addresses = if (physicalNet != null) {
                        physicalNet.getAllByName(domain)
                    } else {
                        InetAddress.getAllByName(domain)
                    }
                    addresses.any { it is Inet4Address }
                } catch (_: Exception) {
                    false
                }
                if (hasIpv4) domain else null
            }
        }.awaitAll().filterNotNull()

        onLog("DNS verification completed: ${verifiedDomains.size} responsive endpoints active on pure network.")

        // 4. Construct Dual Strategy Candidates (ADDR_SNI and STANDARD_SNI)
        val vpsDomain = RouteScannerEngine.DEFAULT_VPS_DOMAIN
        val vpsIp = RouteScannerEngine.DEFAULT_VPS_IP

        val candidates = verifiedDomains.flatMap { domain ->
            val cat = when {
                domain.contains("zoom") || domain.contains("teams") || domain.contains("office") || domain.contains("microsoft") || domain.contains("aka.ms") || domain.contains("ac.lk") -> SniCategory.ZOOM_TEAMS
                domain.contains("tiktok") || domain.contains("byte") -> SniCategory.TIKTOK
                domain.contains("whatsapp") || domain.contains("face") || domain.contains("insta") -> SniCategory.WHATSAPP_SOCIAL
                domain.contains("cloudflare") || domain.contains("one.one") -> SniCategory.CLOUDFLARE_EDGE
                else -> SniCategory.CUSTOM
            }

            listOf(
                // Strategy 1: Addr SNI Fronting
                // Destination IP/Host is the zero-rated bug host (e.g. aka.ms / teams.microsoft.com), TLS SNI is VPS domain
                SniRouteCandidate(
                    domain = domain,
                    strategy = RouteStrategy.ADDR_SNI,
                    category = cat,
                    targetHost = domain,
                    targetPort = 443,
                    sni = vpsDomain,
                    wsHost = vpsDomain,
                    wsPath = "/v2rayops"
                ),
                // Strategy 2: Standard SNI
                // Destination IP is the VPS IP directly, TLS SNI is the bug host domain
                SniRouteCandidate(
                    domain = domain,
                    strategy = RouteStrategy.STANDARD_SNI,
                    category = cat,
                    targetHost = vpsIp,
                    targetPort = 443,
                    sni = domain,
                    wsHost = vpsDomain,
                    wsPath = "/v2rayops"
                )
            )
        }

        onLog("Generated ${candidates.size} dual-strategy routing candidates for deep audit.")
        candidates
    }

    /**
     * Connects to a seed host over TLS and extracts Subject Alternative Names (SANs) from its X.509 certificate.
     */
    private fun harvestSanFromHost(host: String, port: Int = 443): List<String> {
        val physicalNet = routeScannerEngine.getUnderlyingPhysicalNetwork()
        val socket = Socket()
        socket.tcpNoDelay = true
        socket.soTimeout = 2500
        try {
            physicalNet?.bindSocket(socket)
        } catch (_: Exception) {}

        socket.connect(InetSocketAddress(host, port), 2500)

        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAll, SecureRandom())

        val ssl = sslContext.socketFactory.createSocket(socket, host, port, true) as SSLSocket
        val sslParams = SSLParameters()
        sslParams.serverNames = listOf(SNIHostName(host))
        ssl.sslParameters = sslParams
        ssl.startHandshake()

        val certs = ssl.session.peerCertificates
        val leaf = certs.firstOrNull() as? X509Certificate ?: return emptyList()

        val sanEntries = leaf.subjectAlternativeNames ?: return emptyList()
        val extractedNames = mutableListOf<String>()

        for (item in sanEntries) {
            // GeneralName type 2 is dNSName
            if (item.size >= 2 && (item[0] as? Int) == 2) {
                val dnsName = item[1] as? String
                if (!dnsName.isNullOrBlank()) {
                    extractedNames.add(dnsName.lowercase(Locale.ROOT))
                }
            }
        }

        try { ssl.close() } catch (_: Exception) {}
        try { socket.close() } catch (_: Exception) {}

        return extractedNames.distinct()
    }
}
