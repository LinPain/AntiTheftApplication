package com.example.zerotrustauth.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.service.AlarmService
import com.example.zerotrustauth.service.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SecurityEventsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = SecurityPrefs(context)
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, 
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.SIM_STATE_CHANGED" -> {
                Log.i("SecurityReceiver", "Device boot or SIM change detected (${intent.action}). Initializing security services.")
                checkSimState(context, prefs)
                startServicesIfLoggedIn(context, prefs)
            }
            "android.intent.action.SCREEN_OFF" -> {
                // Potential placeholder for tracking lock state
            }
            // Note: Detecting failed unlock attempts specifically usually requires DeviceAdmin
            // or AccessibilityService. For this showcase, we'll simulate or use a 
            // simplified approach if possible, but ACTION_USER_PRESENT (success)
            // can be used to reset the counter.
            Intent.ACTION_USER_PRESENT -> {
                scope.launch {
                    prefs.resetFailedUnlock()
                    prefs.updateInternetTimestamp(System.currentTimeMillis())
                }
            }
        }
    }

    private fun checkSimState(context: Context, prefs: SecurityPrefs) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        val currentSimId = try {
            tm.simOperatorName ?: "unknown"
        } catch (e: SecurityException) {
            "restricted"
        }
        
        scope.launch {
            val lastSimId = prefs.lastSimId.first()
            if (lastSimId != null && lastSimId != currentSimId && currentSimId != "unknown" && currentSimId != "restricted") {
                Log.w("SecurityReceiver", "SIM CARD CHANGED! Potential risk detected.")
                
                // Trigger Comprehensive SIM Alert
                com.example.zerotrustauth.logic.SimManager.notifySimChange(context, currentSimId)

                // Automatically increase risk score
                for (i in 1..4) { 
                    prefs.incrementFailedUnlock()
                }
                // Trigger immediate risk evaluation
                com.example.zerotrustauth.logic.RiskManager.checkAndNotify(context)
            }
            prefs.saveSimId(currentSimId)
        }
    }

    private fun startServicesIfLoggedIn(context: Context, prefs: SecurityPrefs) {
        scope.launch {
            val token = prefs.authToken.first()
            if (!token.isNullOrBlank()) {
                Log.i("SecurityReceiver", "User is logged in. Starting background services...")
                
                // Start AlarmService (Emergency commands)
                val alarmIntent = Intent(context, AlarmService::class.java)
                startServiceForeground(context, alarmIntent)

                // Start LocationService (Live tracking) if enabled
                val isTrackingEnabled = prefs.isLiveTrackingEnabled.first()
                if (isTrackingEnabled) {
                    Log.i("SecurityReceiver", "Live tracking is enabled. Starting LocationService...")
                    val locationIntent = Intent(context, LocationService::class.java)
                    startServiceForeground(context, locationIntent)
                }
            } else {
                Log.d("SecurityReceiver", "No logged in user found. Skipping service auto-start.")
            }
        }
    }

    private fun startServiceForeground(context: Context, intent: Intent) {
        try {
            // Check for background location permission if it's the LocationService
            if (intent.component?.className == LocationService::class.java.name) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("SecurityReceiver", "Skipping LocationService start: ACCESS_BACKGROUND_LOCATION not granted")
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("SecurityReceiver", "Failed to start service: ${e.message}")
        }
    }
}
