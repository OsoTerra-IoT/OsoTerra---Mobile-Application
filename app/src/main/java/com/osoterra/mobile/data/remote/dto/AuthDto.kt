package com.osoterra.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("expiresIn") val expiresIn: Long,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val fullName: String,
    val email: String,

    val role: String,
)
