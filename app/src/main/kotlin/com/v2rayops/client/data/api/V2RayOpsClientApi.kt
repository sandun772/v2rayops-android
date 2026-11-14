package com.v2rayops.client.data.api

import com.v2rayops.client.data.api.dto.DevReportRequestDto
import com.v2rayops.client.data.api.dto.DevReportResponseDto
import com.v2rayops.client.data.api.dto.HeartbeatRequestDto
import com.v2rayops.client.data.api.dto.HeartbeatResponseDto
import com.v2rayops.client.data.api.dto.ProfileResponseDto
import com.v2rayops.client.data.api.dto.RemoteConfigAckRequestDto
import com.v2rayops.client.data.api.dto.RemoteConfigAckResponseDto
import com.v2rayops.client.data.api.dto.RemoteSettingsResponseDto
import com.v2rayops.client.data.api.dto.ServersResponseDto
import com.v2rayops.client.data.api.dto.StructuredIncidentReportDto
import com.v2rayops.client.data.api.dto.SubscriptionResponseDto
import com.v2rayops.client.data.api.dto.SupportResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface V2RayOpsClientApi {

    @GET("api/client/profile")
    suspend fun getProfile(
        @Query("code") customerCode: String
    ): Response<ProfileResponseDto>

    @GET("api/client/subscription")
    suspend fun getSubscription(
        @Query("code") customerCode: String,
        @Query("format") format: String = "json"
    ): Response<SubscriptionResponseDto>

    @GET("api/client/remote-settings")
    suspend fun getRemoteSettings(
        @Query("code") customerCode: String,
        @Query("deviceId") deviceId: String? = null,
        @Query("version") currentVersion: Long = 0L
    ): Response<RemoteSettingsResponseDto>

    @POST("api/client/remote-settings/ack")
    suspend fun acknowledgeRemoteSettings(
        @Body request: RemoteConfigAckRequestDto
    ): Response<RemoteConfigAckResponseDto>

    @GET("api/client/servers")
    suspend fun getServers(): Response<ServersResponseDto>

    @POST("api/client/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: HeartbeatRequestDto
    ): Response<HeartbeatResponseDto>

    @GET("api/client/support")
    suspend fun getSupportContacts(): Response<SupportResponseDto>

    @POST("api/client/dev-report")
    suspend fun uploadDevReport(
        @Body report: DevReportRequestDto
    ): Response<DevReportResponseDto>

    @POST("api/client/dev-report")
    suspend fun uploadIncidentReport(
        @Body report: StructuredIncidentReportDto
    ): Response<DevReportResponseDto>
}
