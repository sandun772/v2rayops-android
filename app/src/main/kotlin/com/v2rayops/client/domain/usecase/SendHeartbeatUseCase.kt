package com.v2rayops.client.domain.usecase

import android.os.Build
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.repository.SubscriptionRepository
import javax.inject.Inject

class SendHeartbeatUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(uploadBytes: Long, downloadBytes: Long): AppResult<Boolean> {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        return subscriptionRepository.sendHeartbeat(uploadBytes, downloadBytes, deviceName)
    }
}
