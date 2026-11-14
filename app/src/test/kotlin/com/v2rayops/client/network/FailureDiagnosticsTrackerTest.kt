package com.v2rayops.client.network

import android.content.Context
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.data.api.V2RayOpsClientApi
import com.v2rayops.client.data.storage.SecureCredentialStore
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FailureDiagnosticsTrackerTest {

    private val context: Context = mockk(relaxed = true)
    private val api: V2RayOpsClientApi = mockk(relaxed = true)
    private val secureStore: SecureCredentialStore = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = AppDispatchers(testDispatcher, testDispatcher, testDispatcher)

    private lateinit var tracker: FailureDiagnosticsTracker

    @Before
    fun setUp() {
        tracker = FailureDiagnosticsTracker(
            context = context,
            api = api,
            secureStore = secureStore,
            networkMonitor = networkMonitor,
            dispatchers = dispatchers
        )
    }

    @Test
    fun initialFailureCount_isZero() {
        assertEquals(0, tracker.getConsecutiveFailures())
    }

    @Test
    fun recordFailure_incrementsConsecutiveCounter() {
        tracker.recordFailure("TIMEOUT_1")
        assertEquals(1, tracker.getConsecutiveFailures())

        tracker.recordFailure("TIMEOUT_2")
        assertEquals(2, tracker.getConsecutiveFailures())
    }

    @Test
    fun recordSuccess_resetsConsecutiveCounterToZero() {
        tracker.recordFailure("HANDSHAKE_FAIL")
        tracker.recordFailure("HANDSHAKE_FAIL")
        assertEquals(2, tracker.getConsecutiveFailures())

        // Healthy connection resets counter completely
        tracker.recordSuccess()
        assertEquals(0, tracker.getConsecutiveFailures())
    }

    @Test
    fun reachingThreshold_reachesThreeConsecutiveFailures() {
        tracker.recordFailure("ERR_1")
        tracker.recordFailure("ERR_2")
        tracker.recordFailure("ERR_3")
        assertEquals(3, tracker.getConsecutiveFailures())
        assertEquals(FailureDiagnosticsTracker.FAILURE_THRESHOLD, tracker.getConsecutiveFailures())
    }
}
