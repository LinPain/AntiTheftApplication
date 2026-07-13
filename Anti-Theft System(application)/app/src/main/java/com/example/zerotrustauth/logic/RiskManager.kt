package com.example.zerotrustauth.logic

import android.content.Context
import android.util.Log
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.RiskAlertRequest
import kotlinx.coroutines.flow.first
import java.time.LocalTime

object RiskManager {
    private var lastAlertScore = 0

    suspend fun checkAndNotify(context: Context) {
        val prefs = SecurityPrefs(context)
        val username = prefs.username.first() ?: return
        val failedUnlockCount = prefs.failedUnlockCount.first()
        val isDeviceTrusted = prefs.isDeviceTrusted.first()
        
        // In a real app, we'd check current location against safeZoneLat/Lon here.
        // For this showcase, we'll assume isKnownLocation is true unless 
        // LocationService explicitly flags it.
        val isKnownLocation = true

        val riskScore = RiskEngine.calculateRiskScore(
            isTrustedDevice = isDeviceTrusted,
            isKnownLocation = isKnownLocation,
            failedUnlockAttempts = failedUnlockCount,
            accessTime = LocalTime.now()
        )

        Log.d("RiskManager", "Checking risk: $riskScore for $username")

        // Trigger alert only if risk is high or significantly increased
        if (riskScore >= 70 && riskScore > lastAlertScore) {
            try {
                Log.w("RiskManager", "CRITICAL RISK DETECTED ($riskScore)! Sending alert...")
                val apiService = LocationApiService.create()
                apiService.notifyRiskAlert(RiskAlertRequest(username, riskScore))
                lastAlertScore = riskScore
            } catch (e: Exception) {
                Log.e("RiskManager", "Failed to send risk alert: ${e.message}")
            }
        } else if (riskScore < 50) {
            // Reset alert tracker if risk drops
            lastAlertScore = 0
        }
    }
}
