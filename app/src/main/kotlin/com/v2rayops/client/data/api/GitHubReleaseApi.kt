package com.v2rayops.client.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "body") val body: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "assets") val assets: List<GitHubAssetDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubAssetDto(
    @Json(name = "name") val name: String,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "browser_download_url") val downloadUrl: String,
    @Json(name = "content_type") val contentType: String? = null
)

interface GitHubReleaseApi {

    @GET("https://api.github.com/repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Accept") accept: String = "application/vnd.github.v3+json",
        @Header("User-Agent") userAgent: String = "V2RayOps-Android"
    ): Response<GitHubReleaseDto>

    @GET
    suspend fun getReleaseFromUrl(
        @Url url: String,
        @Header("Accept") accept: String = "application/vnd.github.v3+json",
        @Header("User-Agent") userAgent: String = "V2RayOps-Android"
    ): Response<GitHubReleaseDto>
}
