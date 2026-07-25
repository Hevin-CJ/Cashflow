package com.hevincj.cashflow.data.remote.models

import com.google.gson.annotations.SerializedName

data class UserProfileResponseDto(
    @SerializedName("username") val username: String,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("phoneNumber") val phoneNumber: String?,
    @SerializedName("profileImage") val profileImage: String?
)
