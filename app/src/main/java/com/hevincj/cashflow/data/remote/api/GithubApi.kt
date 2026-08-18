package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.GithubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = "Hevin-CJ",
        @Path("repo") repo: String = "Cashflow"
    ): GithubReleaseDto
}
