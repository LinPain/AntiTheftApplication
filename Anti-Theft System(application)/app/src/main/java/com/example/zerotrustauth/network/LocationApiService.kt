package com.example.zerotrustauth.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Header
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.Interceptor

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

data class LostModeState(
    val active: Boolean,
    val message: String? = null,
    val phoneNumber: String? = null,
    val timestamp: Long = 0
)

data class FullStatus(
    val alarm: AlarmStatus,
    val lockdown: AlarmStatus,
    val lostMode: LostModeState? = null,
    val trackRequest: AlarmStatus? = null
)

// Auth Requests
data class RegisterRequest(val username: String, val email: String, val password: String)
data class LoginRequest(val username: String, val password: String, val riskScore: Int = 0)
data class VerifyOtpRequest(val username: String, val otp: String)
data class RiskAlertRequest(val username: String, val riskScore: Int)
data class SimAlertRequest(val username: String, val operatorName: String)
data class IntruderRequest(val imageBase64: String, val latitude: Double, val longitude: Double)
data class AuthResponse(
    val message: String,
    val token: String? = null,
    val username: String? = null,
    val mfaRequired: Boolean = false,
    val lockdownRequired: Boolean = false,
    val verificationRequired: Boolean = false
)

interface LocationApiService {
    // Auth APIs - No token needed or provided separately if required
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): AuthResponse

    @POST("api/auth/verify-registration")
    suspend fun verifyRegistration(@Body request: VerifyOtpRequest): AuthResponse

    @POST("api/auth/alert-risk")
    suspend fun notifyRiskAlert(
        @Body request: RiskAlertRequest
    ): AuthResponse

    @POST("api/auth/alert-sim")
    suspend fun notifySimAlert(
        @Body request: SimAlertRequest
    ): AuthResponse

    @POST("api/{username}/intruder")
    suspend fun uploadIntruderLog(
        @Path("username") username: String,
        @Body request: IntruderRequest
    ): AuthResponse

    // Location APIs - Namespaced by username
    @POST("api/{username}/location")
    suspend fun sendLocation(
        @Path("username") username: String,
        @Body location: LocationRequest
    )

    @GET("api/{username}/location/{deviceId}")
    suspend fun getLocationHistory(
        @Path("username") username: String,
        @Path("deviceId") deviceId: String
    ): List<LocationResponse>

    @GET("api/{username}/alarm")
    suspend fun checkAlarm(@Path("username") username: String): AlarmStatus

    @GET("api/{username}/status")
    suspend fun checkFullStatus(@Path("username") username: String): FullStatus

    companion object {
        // Updated to use ngrok for global connectivity (works on 4G/LTE/Any Wi-Fi)
        private const val BASE_URL = "https://pardon-resolute-outscore.ngrok-free.dev/"

        fun create(authToken: String? = null): LocationApiService {
            val clientBuilder = OkHttpClient.Builder()
            
            clientBuilder.addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                requestBuilder.addHeader("ngrok-skip-browser-warning", "true")
                
                if (authToken != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $authToken")
                }
                
                chain.proceed(requestBuilder.build())
            }

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LocationApiService::class.java)
        }
    }
}
