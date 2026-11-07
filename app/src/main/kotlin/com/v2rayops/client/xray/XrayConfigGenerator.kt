package com.v2rayops.client.xray

import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.UserSettings
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.profiles.ProfileParser
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XrayConfigGenerator @Inject constructor(
    private val profileParser: ProfileParser
) {
    /**
     * Generates a fully formatted, valid Xray Core configuration JSON.
     */
    fun generateJson(config: VpnConfig, settings: UserSettings): String {
        val root = JSONObject()

        // 1. Log configuration
        val log = JSONObject().apply {
            put("loglevel", "debug")
        }
        root.put("log", log)

        val parsed = profileParser.parseUri(config.configUri)

        val isIp = { s: String -> s.matches(Regex("^[0-9.]+$")) || s.contains(":") }
        val isVpsHost = { h: String ->
            h.contains("vpnxhub.com", ignoreCase = true) || h == "172.236.134.215"
        }

        // Determine if this is an "Addr-as-SNI" (Fronting) config:
        // URI address is a bug host (support.zoom.us, tiktok.com, etc.), while wsHost/sni is our VPS cluster
        val isAddrSniFronting = parsed != null &&
                !isVpsHost(parsed.host) &&
                (isVpsHost(parsed.wsHost) || isVpsHost(parsed.sni))

        val outboundAddress = if (parsed != null) {
            if (isAddrSniFronting || isVpsHost(parsed.host)) {
                "172.236.134.215"
            } else {
                parsed.host
            }
        } else {
            "172.236.134.215"
        }

        // 2. DNS
        val dns = JSONObject().apply {
            val servers = JSONArray().apply {
                put("localhost")
                put(settings.customDns.ifBlank { "1.1.1.1" })
                put("8.8.8.8")
                put("tcp://1.1.1.1")
            }
            put("servers", servers)
            put("queryStrategy", "UseIPv4")
            val hosts = JSONObject().apply {
                put("sg-v2rayops.vpnxhub.com", "172.236.134.215")
                put("v2rayops.vpnxhub.com", "172.236.134.215")
                put("dns.google", "8.8.8.8")
                put("one.one.one.one", "1.1.1.1")
                if (parsed != null && parsed.host.isNotBlank() && !isIp(parsed.host)) {
                    put(parsed.host, outboundAddress)
                }
            }
            put("hosts", hosts)
        }
        root.put("dns", dns)

        // 3. Inbounds (Native TUN + Local SOCKS & HTTP)
        val effectiveMtu = if (settings.cellularMtuOptimized) 1380 else 1500
        val inbounds = JSONArray().apply {
            // Layer 3 TUN inbound for native Android VpnService capture
            put(JSONObject().apply {
                put("tag", "tun")
                put("port", 0)
                put("protocol", "tun")
                put("settings", JSONObject().apply {
                    put("name", "xray0")
                    put("mtu", effectiveMtu)
                    put("userLevel", 8)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                        put("quic")
                    })
                    put("routeOnly", true)
                })
            })

            // Local SOCKS inbound
            put(JSONObject().apply {
                put("tag", "socks")
                put("port", 10808)
                put("listen", "127.0.0.1")
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                    put("userLevel", 8)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                        put("quic")
                    })
                })
            })

            // Local HTTP inbound
            put(JSONObject().apply {
                put("tag", "http")
                put("port", 10809)
                put("listen", "127.0.0.1")
                put("protocol", "http")
            })
        }
        root.put("inbounds", inbounds)

        // 4. Outbounds (Proxy Outbound + Direct + Block)
        val outbounds = JSONArray()

        val proxyOutbound = JSONObject().apply {
            put("tag", "proxy")
            put("protocol", config.protocol.lowercase())

            val settingsObj = JSONObject()
            if (parsed != null) {
                if (config.isVless) {
                    val vnext = JSONArray().apply {
                        put(JSONObject().apply {
                            put("address", outboundAddress)
                            put("port", parsed.port)
                            val users = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("id", parsed.uuidOrPassword)
                                    put("encryption", "none")
                                    if (parsed.flow.isNotBlank()) put("flow", parsed.flow)
                                })
                            }
                            put("users", users)
                        })
                    }
                    settingsObj.put("vnext", vnext)
                } else if (config.isTrojan) {
                    val servers = JSONArray().apply {
                        put(JSONObject().apply {
                            put("address", outboundAddress)
                            put("port", parsed.port)
                            put("password", parsed.uuidOrPassword)
                        })
                    }
                    settingsObj.put("servers", servers)
                } else if (config.isVmess) {
                    val vnext = JSONArray().apply {
                        put(JSONObject().apply {
                            put("address", outboundAddress)
                            put("port", parsed.port)
                            val users = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("id", parsed.uuidOrPassword)
                                    put("alterId", 0)
                                    put("security", "auto")
                                })
                            }
                            put("users", users)
                        })
                    }
                    settingsObj.put("vnext", vnext)
                }
            }
            // Enable XUDP packet encoding for high-speed UDP tunneling over WebSocket streams (critical for DNS & QUIC)
            settingsObj.put("packetEncoding", "xudp")
            put("settings", settingsObj)

            // StreamSettings (Reality / TLS / WebSocket)
            if (parsed != null) {
                val effectiveSni = if (isAddrSniFronting) {
                    parsed.host // In Addr-Fronting, bug host from address is sent as zero-rated SNI
                } else {
                    parsed.sni.ifBlank { parsed.host }
                }

                val effectiveWsHost = if (isAddrSniFronting) {
                    if (isVpsHost(parsed.wsHost)) parsed.wsHost else if (isVpsHost(parsed.sni)) parsed.sni else "sg-v2rayops.vpnxhub.com"
                } else {
                    parsed.wsHost.ifBlank { parsed.host }
                }

                val streamSettings = JSONObject().apply {
                    put("network", parsed.networkType.ifBlank { "tcp" })
                    if (parsed.security.isNotBlank() && parsed.security != "none") {
                        put("security", parsed.security)
                    }

                    if (parsed.security == "reality") {
                        val realitySettings = JSONObject().apply {
                            put("serverName", effectiveSni)
                            put("publicKey", parsed.publicKey)
                            put("shortId", parsed.shortId)
                            put("fingerprint", "chrome")
                        }
                        put("realitySettings", realitySettings)
                    } else if (parsed.security == "tls") {
                        val candidateDomains = linkedSetOf<String>()

                        val addDomainWithWildcards: (String) -> Unit = { raw ->
                            if (raw.isNotBlank() && !isIp(raw)) {
                                candidateDomains.add(raw)
                                val parts = raw.split(".")
                                if (parts.size >= 2) {
                                    val rootDomain = parts.takeLast(2).joinToString(".")
                                    candidateDomains.add(rootDomain)
                                    candidateDomains.add("*.$rootDomain")
                                    if (parts.size >= 3) {
                                        val parent = parts.drop(1).joinToString(".")
                                        candidateDomains.add(parent)
                                        candidateDomains.add("*.$parent")
                                    }
                                }
                            }
                        }

                        addDomainWithWildcards(effectiveWsHost)
                        addDomainWithWildcards(parsed.host)
                        addDomainWithWildcards(effectiveSni)

                        // Cluster origin & CDN fallback cert identities
                        candidateDomains.add("sg-v2rayops.vpnxhub.com")
                        candidateDomains.add("v2rayops.vpnxhub.com")
                        candidateDomains.add("vpnxhub.com")
                        candidateDomains.add("*.vpnxhub.com")

                        val tlsSettings = JSONObject().apply {
                            put("serverName", effectiveSni)
                            if (parsed.allowInsecure || isAddrSniFronting || effectiveSni != outboundAddress) {
                                put("allowInsecure", true)
                            }
                            if (candidateDomains.isNotEmpty()) {
                                put("verifyPeerCertByName", candidateDomains.joinToString(","))
                            }
                            put("fingerprint", "chrome")
                            if (parsed.networkType.equals("ws", ignoreCase = true)) {
                                put("alpn", JSONArray().apply {
                                    put("http/1.1")
                                })
                            }
                        }
                        put("tlsSettings", tlsSettings)
                    }

                    // WebSocket transport settings (standard V2RayOPS 3X-UI transport)
                    if (parsed.networkType.equals("ws", ignoreCase = true)) {
                        val wsSettings = JSONObject().apply {
                            put("path", parsed.wsPath.ifBlank { "/v2rayops" })
                            put("host", effectiveWsHost)
                            val headers = JSONObject().apply {
                                put("Host", effectiveWsHost)
                            }
                            put("headers", headers)
                        }
                        put("wsSettings", wsSettings)
                    }

                    // DPI Evasion & Anti-Jitter Socket Options
                    val sockopt = JSONObject().apply {
                        put("tcpNoDelay", true)
                        put("tcpKeepAliveInterval", 15)
                        if (settings.dpiDelayFixEnabled) {
                            put("dialerProxy", "fragment")
                        }
                    }
                    put("sockopt", sockopt)
                }
                put("streamSettings", streamSettings)
            }
        }
        outbounds.put(proxyOutbound)

        // DPI Evasion & Jitter Fix Outbound (TLS Packet Fragmentation)
        if (settings.dpiDelayFixEnabled) {
            outbounds.put(JSONObject().apply {
                put("tag", "fragment")
                put("protocol", "freedom")
                put("settings", JSONObject().apply {
                    put("domainStrategy", "AsIs")
                    put("fragment", JSONObject().apply {
                        put("packets", "1-3")
                        put("length", "100-200")
                        put("interval", "10-20")
                    })
                })
                put("streamSettings", JSONObject().apply {
                    put("sockopt", JSONObject().apply {
                        put("tcpNoDelay", true)
                        put("tcpKeepAliveInterval", 15)
                    })
                })
            })
        }

        // Direct Outbound
        outbounds.put(JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
        })

        // Block Outbound
        outbounds.put(JSONObject().apply {
            put("tag", "block")
            put("protocol", "blackhole")
        })

        // DNS Outbound
        outbounds.put(JSONObject().apply {
            put("tag", "dns-out")
            put("protocol", "dns")
        })

        root.put("outbounds", outbounds)

        // 5. Routing
        val routing = JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            val rules = JSONArray()

            // DNS Interception: route UDP/TCP port 53 to Xray DNS engine
            rules.put(JSONObject().apply {
                put("type", "field")
                put("inboundTag", JSONArray().apply {
                    put("tun")
                    put("tun-in")
                    put("socks")
                })
                put("port", "53")
                put("network", "tcp,udp")
                put("outboundTag", "dns-out")
            })

            // Bypass Private LAN
            rules.put(JSONObject().apply {
                put("type", "field")
                put("outboundTag", "direct")
                put("ip", JSONArray().apply {
                    put("geoip:private")
                })
            })

            // Bypass Sri Lanka if configured
            if (settings.routingMode == RoutingMode.BYPASS_LAN_AND_SRI_LANKA) {
                rules.put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "direct")
                    put("ip", JSONArray().apply {
                        put("geoip:lk")
                    })
                })
            }

            // Route all captured TUN and local proxy traffic to proxy outbound
            rules.put(JSONObject().apply {
                put("type", "field")
                put("inboundTag", JSONArray().apply {
                    put("tun")
                    put("tun-in")
                    put("socks")
                    put("socks-in")
                    put("http")
                    put("http-in")
                })
                put("outboundTag", "proxy")
            })

            // Default proxy rule
            rules.put(JSONObject().apply {
                put("type", "field")
                put("outboundTag", "proxy")
                put("network", "tcp,udp")
            })

            put("rules", rules)
        }
        root.put("routing", routing)

        // 6. Real-Time Outbound Traffic Statistics & Adaptive QoS Policy
        root.put("stats", JSONObject())
        val policy = JSONObject().apply {
            val levels = JSONObject().apply {
                // Level 0: Interactive & Low-Latency Tier
                val level0 = JSONObject().apply {
                    put("handshake", 4)
                    put("connIdle", 300)
                    put("uplinkOnly", 1)
                    put("downlinkOnly", 1)
                    put("bufferSize", 16)
                }
                put("0", level0)

                // Level 8: Standard / Smart QoS Tier
                val level8 = JSONObject().apply {
                    put("handshake", 4)
                    put("connIdle", 300)
                    put("uplinkOnly", 1)
                    put("downlinkOnly", 1)
                    // Smart QoS: Restrict buffer size to apply TCP sliding window backpressure and mitigate bufferbloat
                    val bufKb = if (settings.smartQoSEnabled) settings.tcpBufferSizeKb.coerceIn(16, 128) else 64
                    put("bufferSize", bufKb)
                }
                put("8", level8)
            }
            put("levels", levels)

            val system = JSONObject().apply {
                put("statsOutboundUplink", true)
                put("statsOutboundDownlink", true)
            }
            put("system", system)
        }
        root.put("policy", policy)

        return root.toString(2)
    }
}
