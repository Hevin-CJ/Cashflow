package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.GithubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = "Hevin-CJ",
        @Path("repo") repo: String = "Cashflow"
    ): GithubReleaseDto

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getAllReleases(
        @Path("owner") owner: String = "Hevin-CJ",
        @Path("repo") repo: String = "Cashflow",
        @Query("per_page") perPage: Int = 15
    ): List<GithubReleaseDto>
}
