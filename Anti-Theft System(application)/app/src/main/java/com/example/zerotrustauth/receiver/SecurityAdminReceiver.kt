package com.example.zerotrustauth.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.logic.RiskManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecurityAdminReceiver : DeviceAdminReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.w("SecurityAdmin", "FAILED SCREEN LOCK ATTEMPT DETECTED!")
        val prefs = SecurityPrefs(context)
        scope.launch {
            prefs.incrementFailedUnlock()
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
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("SecurityAdmin", "Device Admin Disabled - Security reduced!")
    }
}
