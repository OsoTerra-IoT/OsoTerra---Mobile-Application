package com.osoterra.mobile.data.remote.api

import com.osoterra.mobile.data.remote.dto.LoginRequestDto
import com.osoterra.mobile.data.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Suppress("unused")
interface OsoTerraApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @GET("plots")
    suspend fun getPlots(): List<Any>

    @GET("plots/{id}")
    suspend fun getPlot(@Path("id") id: String): Any

    @GET("plots/{id}/readings")
    suspend fun getReadings(
        @Path("id") id: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<Any>

    @GET("alerts")
    suspend fun getAlerts(): List<Any>
}
