package com.hevincj.cashflow.domain.models

data class UserProfile(
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val profileImage: String?
)
