package com.example.zerotrustauth.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Header
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.Interceptor

data class LocationRequest(
    val deviceId: String,
    val deviceName: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Rich Device Info
    val batteryLevel: Int? = null,
    val isCharging: Boolean? = null,
    val networkType: String? = null,
    val carrier: String? = null,
    val ipAddress: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val androidVersion: String? = null,
    val apiLevel: Int? = null,
    val isRooted: Boolean? = null,
    val isEncryptionEnabled: Boolean? = null,
    val isDeveloperMode: Boolean? = null,
    val isUsbDebuggingEnabled: Boolean? = null,
    val riskScore: Int? = null
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
data class ForgotPasswordRequest(val identifier: String)
data class VerifyResetRequest(val username: String, val otp: String)
data class ResetPasswordRequest(val resetToken: String, val newPassword: String)
data class VerifyOtpRequest(val username: String, val otp: String)
data class ResendOtpRequest(val username: String, val type: String)
data class RiskAlertRequest(val username: String, val riskScore: Int)
data class SimAlertRequest(val username: String, val operatorName: String)
data class GenericAlertRequest(val event: String, val details: Map<String, Any>)
data class AuthResponse(
    val message: String,
    val token: String? = null,
    val resetToken: String? = null,
    val username: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val mockCode: String? = null,
    val mfaRequired: Boolean = false,
    val lockdownRequired: Boolean = false,
    val verificationRequired: Boolean = false
)

data class DeviceStatusResponse(
    val _id: String, // deviceId
    val deviceName: String?,
    val lastLatitude: Double,
    val lastLongitude: Double,
    val lastTimestamp: String
)

data class SecurityEventRequest(val eventType: String, val details: String? = null)
data class FlashlightRequest(val active: Boolean)

data class LostModeRequest(val active: Boolean, val message: String? = null, val phoneNumber: String? = null)

interface LocationApiService {
    // Auth APIs
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): AuthResponse

    @POST("api/auth/verify-reset")
    suspend fun verifyReset(@Body request: VerifyResetRequest): AuthResponse

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): AuthResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): AuthResponse

    @POST("api/auth/verify-registration")
    suspend fun verifyRegistration(@Body request: VerifyOtpRequest): AuthResponse

    @POST("api/auth/resend-otp")
    suspend fun resendOtp(@Body request: ResendOtpRequest): AuthResponse

    @POST("api/auth/alert-risk")
    suspend fun notifyRiskAlert(
        @Body request: RiskAlertRequest
    ): AuthResponse

    @POST("api/auth/alert-sim")
    suspend fun notifySimAlert(
        @Body request: SimAlertRequest
    ): AuthResponse

    @POST("api/{username}/alert-generic")
    suspend fun notifyGenericAlert(
        @Path("username") username: String,
        @Body request: GenericAlertRequest
    ): AuthResponse

    @POST("api/{username}/security-events")
    suspend fun reportSecurityEvent(
        @Path("username") username: String,
        @Body request: SecurityEventRequest
    ): AuthResponse

    @GET("api/{username}/location/devices/status")
    suspend fun getDeviceList(@Path("username") username: String): List<DeviceStatusResponse>

    @DELETE("api/{username}/location/{deviceId}")
    suspend fun removeDevice(
        @Path("username") username: String,
        @Path("deviceId") deviceId: String
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

    @POST("api/{username}/lost-mode")
    suspend fun setLostMode(
        @Path("username") username: String,
        @Body request: LostModeRequest
    ): AuthResponse

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
