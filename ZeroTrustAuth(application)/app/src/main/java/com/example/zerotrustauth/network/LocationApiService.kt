package com.example.zerotrustauth.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class LocationRequest(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double
)

data class LocationResponse(
    val _id: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)

data class AlarmStatus(
    val active: Boolean,
    val timestamp: Long
)

data class FullStatus(
    val alarm: AlarmStatus,
    val lockdown: AlarmStatus
)

// Auth Requests
data class RegisterRequest(val username: String, val email: String, val password: String)
data class LoginRequest(val username: String, val password: String)
data class VerifyOtpRequest(val username: String, val otp: String)
data class AuthResponse(val message: String, val token: String?, val username: String?)

interface LocationApiService {
    // Auth APIs
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): AuthResponse

    // Location APIs
    @POST("api/location")
    suspend fun sendLocation(@Body location: LocationRequest)

    @GET("api/location/{deviceId}")
    suspend fun getLocationHistory(@Path("deviceId") deviceId: String): List<LocationResponse>

    @GET("api/alarm")
    suspend fun checkAlarm(): AlarmStatus

    @GET("api/status")
    suspend fun checkFullStatus(): FullStatus

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3000/"

        fun create(): LocationApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LocationApiService::class.java)
        }
    }
}