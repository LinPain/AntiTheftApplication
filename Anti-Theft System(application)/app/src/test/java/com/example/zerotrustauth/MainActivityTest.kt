package com.example.zerotrustauth

import com.example.zerotrustauth.logic.RiskEngine
import com.example.zerotrustauth.logic.SecurityLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class MainActivityTest {

    @Test
    fun `test low risk score with trusted device and known location`() {
        val score = RiskEngine.calculateRiskScore(
            isTrustedDevice = true,
            isKnownLocation = true,
            accessTime = LocalTime.of(10, 0) // Ban ngày (An toàn)
        )
        assertEquals(0, score)
        assertEquals(SecurityLevel.LOW, RiskEngine.getSecurityLevel(score))
    }

    @Test
    fun `test medium risk score with untrusted device`() {
        val score = RiskEngine.calculateRiskScore(
            isTrustedDevice = false,
            isKnownLocation = true,
            accessTime = LocalTime.of(10, 0)
        )
        // Kết quả mong đợi: 40 điểm rủi ro
        assertEquals(40, score)
        assertEquals(SecurityLevel.MEDIUM, RiskEngine.getSecurityLevel(score))
    }

    @Test
    fun `test high risk score with untrusted device and location at midnight`() {
        val score = RiskEngine.calculateRiskScore(
            isTrustedDevice = false,
            isKnownLocation = false,
            accessTime = LocalTime.of(2, 0)
        )
        assertEquals(90, score)
        assertEquals(SecurityLevel.HIGH, RiskEngine.getSecurityLevel(score))
    }
}
