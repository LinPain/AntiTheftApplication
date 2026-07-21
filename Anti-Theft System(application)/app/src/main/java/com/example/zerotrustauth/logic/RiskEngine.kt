package com.example.zerotrustauth.logic

import java.time.LocalTime

/**
 * Optimized Risk Engine for Zero Trust Authentication
 */
object RiskEngine {
    
    fun calculateRiskScore(
        isTrustedDevice: Boolean,
        isOutsideSafeZone: Boolean,
        hasSimChanged: Boolean = false,
        failedUnlockAttempts: Int = 0,
        offlineDurationHours: Long = 0,
        accessTime: LocalTime = LocalTime.now()
    ): Int {
        var score = 0
        
        // 1. Device Trust (+40 if unknown)
        if (!isTrustedDevice) score += 40
        
        // 2. Geofence / Location (+30 if outside trusted zone)
        if (isOutsideSafeZone) score += 30
        
        // 3. Time anomaly (+20 for midnight access)
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
    LOW, MEDIUM, HIGH, CRITICAL
}
