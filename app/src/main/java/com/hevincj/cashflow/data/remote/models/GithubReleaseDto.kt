package com.hevincj.cashflow.data.remote.models

import com.google.gson.annotations.SerializedName

data class GithubReleaseDto(
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("body")
    val body: String?,
    @SerializedName("published_at")
    val publishedAt: String?,
    @SerializedName("html_url")
    val htmlUrl: String?,
    @SerializedName("assets")
    val assets: List<GithubReleaseAssetDto> = emptyList()
)

data class GithubReleaseAssetDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("size")
    val size: Long,
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    @SerializedName("content_type")
    val contentType: String?
)
