package com.example.zerotrustauth.logic

import java.time.LocalTime

/**
 * Enhanced Risk Engine for Zero Trust Authentication
 */
object RiskEngine {
    
    fun calculateRiskScore(
        isTrustedDevice: Boolean,
        isKnownLocation: Boolean,
        hasSimChanged: Boolean = false,
        failedUnlockAttempts: Int = 0,
        offlineDurationHours: Long = 0,
        accessTime: LocalTime = LocalTime.now()
    ): Int {
        var score = 0
        
        // 1. Device Trust (+40 if unknown)
        if (!isTrustedDevice) score += 40
        
        // 2. Location (+30 if strange/unknown)
        if (!isKnownLocation) score += 30
        
        // 3. Time anomaly (e.g., midnight access +20)
        if (accessTime.isAfter(LocalTime.MIDNIGHT) && accessTime.isBefore(LocalTime.of(5, 0))) {
            score += 20
        }

        // 4. SIM Card Change (+50 - high risk)
        if (hasSimChanged) score += 50

        // 5. Failed Unlock Attempts (+15 per attempt after 2)
        if (failedUnlockAttempts > 2) {
            score += (failedUnlockAttempts - 2) * 15
        }

        // 6. Offline Duration (+10 per day offline)
        if (offlineDurationHours > 24) {
            score += (offlineDurationHours / 24).toInt() * 10
        }
        
        // Cap score at 100
        return score.coerceAtMost(100)
    }

    fun getSecurityLevel(score: Int): SecurityLevel {
        return when {
            score <= 20 -> SecurityLevel.LOW
            score <= 50 -> SecurityLevel.MEDIUM
            score <= 80 -> SecurityLevel.HIGH
            else -> SecurityLevel.CRITICAL
        }
    }
}

enum class SecurityLevel {
    LOW,      // Password only
    MEDIUM,   // Password + MFA
    HIGH,     // MFA + Biometric
    CRITICAL  // Access Denied / Lockdown
}
