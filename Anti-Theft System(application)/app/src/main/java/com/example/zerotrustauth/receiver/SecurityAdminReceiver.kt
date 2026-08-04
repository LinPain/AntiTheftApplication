package com.example.zerotrustauth.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.logic.RiskManager
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.SecurityEventRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SecurityAdminReceiver : DeviceAdminReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.w("SecurityAdmin", "FAILED SCREEN LOCK ATTEMPT DETECTED!")
        val prefs = SecurityPrefs(context)
        scope.launch {
            prefs.incrementFailedUnlock()
            
            val failures = prefs.failedUnlockCount.first()
            
            scope.launch {
                val token = prefs.authToken.first()
                val user = prefs.username.first()?.lowercase()
                if (token != null && user != null) {
                    LocationApiService.create(token).reportSecurityEvent(user, SecurityEventRequest("FAILED_PIN_ATTEMPT", "Attempts: $failures"))
                }
            }
            
            // Evaluate risk and send alert if needed
            RiskManager.checkAndNotify(context)
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Log.i("SecurityAdmin", "Successful screen lock unlock. Resetting risk counter.")
        val prefs = SecurityPrefs(context)
        scope.launch {
            prefs.resetFailedUnlock()
            prefs.updateInternetTimestamp(System.currentTimeMillis())
            // Reset alert tracker
            RiskManager.checkAndNotify(context)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("SecurityAdmin", "Device Admin Enabled")
        
        // Allowlist this app for Lock Task Mode to prevent escaping via Back + Overview
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(context, SecurityAdminReceiver::class.java)
        try {
            dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // Disable all system UI features when in lock task mode for maximum security
                dpm.setLockTaskFeatures(adminComponent, android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
        } catch (e: Exception) {
            Log.e("SecurityAdmin", "Failed to set lock task packages: ${e.message}")
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("SecurityAdmin", "Device Admin Disabled - Security reduced!")
    }
}
