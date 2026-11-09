package com.v2rayops.client.domain.model

data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val changelog: String,
    val downloadUrl: String,
    val apkSizeBytes: Long,
    val hasUpdate: Boolean,
    val releaseDate: String? = null
)
