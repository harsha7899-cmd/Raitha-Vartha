package com.example.raitha_vartha

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class OtpRequest(
    val email: String,
    val phone: String,
    val otp: String
)

data class OtpResponse(
    val success: Boolean,
    val message: String
)

interface OtpApiService {
    @POST("send-otp") 
    suspend fun sendOtp(@Body request: OtpRequest): Response<OtpResponse>

    companion object {
        private const val BASE_URL = "https://your-api-service.com/" 

        fun isPlaceholderUrl(): Boolean = BASE_URL.contains("your-api-service.com")

        fun create(): OtpApiService {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OtpApiService::class.java)
        }
    }
}
