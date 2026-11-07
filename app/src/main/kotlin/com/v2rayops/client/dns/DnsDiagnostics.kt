package com.v2rayops.client.dns

import com.v2rayops.client.core.dispatchers.AppDispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

data class DnsDiagnosticResult(
    val domain: String,
    val resolvedIps: List<String>,
    val resolutionTimeMs: Long,
    val isSuccess: Boolean,
    val error: String? = null
)

@Singleton
class DnsDiagnostics @Inject constructor(
    private val dispatchers: AppDispatchers
) {
    suspend fun resolveDomain(domain: String): DnsDiagnosticResult = withContext(dispatchers.io) {
        val start = System.currentTimeMillis()
        try {
            val addresses = InetAddress.getAllByName(domain)
            val elapsed = System.currentTimeMillis() - start
            DnsDiagnosticResult(
                domain = domain,
                resolvedIps = addresses.map { it.hostAddress ?: "" },
                resolutionTimeMs = elapsed,
                isSuccess = true
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            DnsDiagnosticResult(
                domain = domain,
                resolvedIps = emptyList(),
                resolutionTimeMs = elapsed,
                isSuccess = false,
                error = e.message
            )
        }
    }
}
