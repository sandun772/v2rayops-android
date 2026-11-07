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
        }
        root.put("dns", dns)

        // 3. Inbounds (Local SOCKS & HTTP for TUN routing)
        val inbounds = JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "socks-in")
                put("port", 10808)
                put("listen", "127.0.0.1")
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                })
            })
            put(JSONObject().apply {
                put("tag", "http-in")
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

            // StreamSettings (Reality / TLS)
            if (parsed != null && (parsed.security == "reality" || parsed.security == "tls")) {
                val streamSettings = JSONObject().apply {
                    put("network", parsed.networkType.ifBlank { "tcp" })
                    put("security", parsed.security)

                    if (parsed.security == "reality") {
                        val realitySettings = JSONObject().apply {
                            put("serverName", parsed.sni)
                            put("publicKey", parsed.publicKey)
                            put("shortId", parsed.shortId)
                            put("fingerprint", "chrome")
                        }
                        put("realitySettings", realitySettings)
                    } else if (parsed.security == "tls") {
                        val tlsSettings = JSONObject().apply {
                            put("serverName", parsed.sni)
                        }
                        put("tlsSettings", tlsSettings)
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

        root.put("outbounds", outbounds)

        // 5. Routing
        val routing = JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            val rules = JSONArray()

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

            // Default proxy rule
            rules.put(JSONObject().apply {
                put("type", "field")
                put("outboundTag", "proxy")
                put("network", "tcp,udp")
            })

            put("rules", rules)
        }
        root.put("routing", routing)

        return root.toString(2)
    }
}
