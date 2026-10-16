package com.osoterra.mobile.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.osoterra.mobile.BuildConfig
import com.osoterra.mobile.data.remote.api.OsoTerraApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
/**
 * Cliente HTTP del RESTful API. Agrega el token de la sesión y, si el Backend lo rechaza
 * (401 con sesión iniciada), avisa con [onUnauthorized] para cerrar la sesión.
 */
class ApiClient(
    private val tokenProvider: () -> String?,
    private val onUnauthorized: () -> Unit = {},
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("Accept-Language", "es-419")
                val token = tokenProvider()
                token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
                val response = chain.proceed(requestBuilder.build())
                if (response.code == 401 && token != null) onUnauthorized()
                response
            }
            .addInterceptor(logging)
            .build()
    }

    val api: OsoTerraApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OsoTerraApi::class.java)
    }
}
