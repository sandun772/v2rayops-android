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
            put("loglevel", "warning")
        }
        root.put("log", log)

        // 2. DNS
        val dns = JSONObject().apply {
            val servers = JSONArray().apply {
                put(settings.customDns.ifBlank { "1.1.1.1" })
                put("8.8.8.8")
            }
            put("servers", servers)
            put("queryStrategy", "UseIPv4")
        }
        root.put("dns", dns)

        // 3. Inbounds (Native TUN + Local SOCKS & HTTP)
        val inbounds = JSONArray().apply {
            // Layer 3 TUN inbound for native Android VpnService capture
            put(JSONObject().apply {
                put("tag", "tun")
                put("port", 0)
                put("protocol", "tun")
                put("settings", JSONObject().apply {
                    put("name", "xray0")
                    put("MTU", 1500)
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
        val parsed = profileParser.parseUri(config.configUri)

        val proxyOutbound = JSONObject().apply {
            put("tag", "proxy")
            put("protocol", config.protocol.lowercase())

            val settingsObj = JSONObject()
            if (config.isVless && parsed != null) {
                val vnext = JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", parsed.host)
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
            }
            put("settings", settingsObj)

            // StreamSettings (Reality / TLS / WebSocket)
            if (parsed != null) {
                val streamSettings = JSONObject().apply {
                    put("network", parsed.networkType.ifBlank { "tcp" })
                    if (parsed.security.isNotBlank() && parsed.security != "none") {
                        put("security", parsed.security)
                    }

                    if (parsed.security == "reality") {
                        val realitySettings = JSONObject().apply {
                            put("serverName", parsed.sni)
                            put("publicKey", parsed.publicKey)
                            put("shortId", parsed.shortId)
                            put("fingerprint", "chrome")
                        }
                        put("realitySettings", realitySettings)
                    } else if (parsed.security == "tls") {
                        val certName = if (parsed.host.contains("vpnxhub.com", ignoreCase = true)) {
                            parsed.host
                        } else if (parsed.sni.contains("vpnxhub.com", ignoreCase = true)) {
                            parsed.sni
                        } else {
                            parsed.host
                        }

                        val tlsSettings = JSONObject().apply {
                            put("serverName", parsed.sni.ifBlank { parsed.host })
                            put("verifyPeerCertByName", certName)
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
                            val hostHeader = parsed.wsHost.ifBlank { parsed.host }
                            put("host", hostHeader)
                            val headers = JSONObject().apply {
                                put("Host", hostHeader)
                            }
                            put("headers", headers)
                        }
                        put("wsSettings", wsSettings)
                    }
                }
                put("streamSettings", streamSettings)
            }
        }
        outbounds.put(proxyOutbound)

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

        // 6. Real-Time Outbound Traffic Statistics & System Policy
        root.put("stats", JSONObject())
        val policy = JSONObject().apply {
            val levels = JSONObject().apply {
                val level8 = JSONObject().apply {
                    put("handshake", 4)
                    put("connIdle", 300)
                    put("uplinkOnly", 1)
                    put("downlinkOnly", 1)
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
