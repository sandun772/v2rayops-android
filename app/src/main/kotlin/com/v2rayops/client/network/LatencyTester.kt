package com.v2rayops.client.network

import com.v2rayops.client.core.dispatchers.AppDispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LatencyTester @Inject constructor(
    private val dispatchers: AppDispatchers
) {
    suspend fun measureTcpLatency(host: String, port: Int, timeoutMs: Int = 3000): Long? =
        withContext(dispatchers.io) {
            try {
                val start = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                }
                System.currentTimeMillis() - start
            } catch (_: Exception) {
                null
            }
        }
}
