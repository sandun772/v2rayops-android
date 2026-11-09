package com.v2rayops.client.core.network

import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.vpn.V2RayOpsVpnService
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

/**
 * Intelligent ProxySelector for OkHttp.
 * When V2RayOps VPN tunnel is running, this routes client HTTP/HTTPS requests
 * through the local Xray proxy (127.0.0.1:10809) so traffic travels through the
 * encrypted VLESS tunnel rather than raw carrier networks where GitHub is throttled/blocked.
 * When the VPN is inactive or for localhost connections, it direct-routes without proxy.
 */
class SmartVpnProxySelector : ProxySelector() {

    private val localHttpProxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 10809))
    private val directList = listOf(Proxy.NO_PROXY)

    override fun select(uri: URI?): List<Proxy> {
        val host = uri?.host
        if (host == null || host == "127.0.0.1" || host == "localhost") {
            return directList
        }

        return if (V2RayOpsVpnService.isRunning) {
            // Try local tunnel proxy first, then direct fallback
            listOf(localHttpProxy, Proxy.NO_PROXY)
        } else {
            directList
        }
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        AppLogger.w("SmartVpnProxySelector", "Connection via proxy $sa for $uri failed: ${ioe?.message}")
    }
}
