package com.example.zerotrustauth

import android.os.Bundle
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zerotrustauth.ui.theme.AntiTheftSystemTheme
import com.example.zerotrustauth.ui.login.LoginScreen
import com.example.zerotrustauth.ui.login.MFAScreen
import com.example.zerotrustauth.ui.register.RegisterScreen
import com.example.zerotrustauth.ui.register.RegisterMFAScreen
import com.example.zerotrustauth.ui.dashboard.*
import com.example.zerotrustauth.ui.location.LocationTrackingScreen
import com.example.zerotrustauth.ui.lockdown.LockdownScreen
import com.example.zerotrustauth.ui.lockdown.LostModeScreen
import com.example.zerotrustauth.ui.history.LocationHistoryScreen
import com.example.zerotrustauth.ui.antitheft.AntiTheftLockScreen
import com.example.zerotrustauth.service.AlarmService
import com.example.zerotrustauth.service.LocationService
import com.example.zerotrustauth.logic.RiskEngine
import com.example.zerotrustauth.logic.SecurityLevel
import androidx.compose.runtime.collectAsState
import com.example.zerotrustauth.data.SecurityPrefs
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity

class MainActivity : FragmentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request Location Permissions at Startup
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        // Start the remote control listener service
        val alarmIntent = Intent(this, AlarmService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(alarmIntent)
        } else {
            startService(alarmIntent)
        }

        // Start LocationService if it was previously enabled
        val securityPrefs = SecurityPrefs(applicationContext)
        val isTrackingEnabled = kotlinx.coroutines.runBlocking { 
            securityPrefs.isLiveTrackingEnabled.first() 
        }
        if (isTrackingEnabled) {
            val locationIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(locationIntent)
            } else {
                startService(locationIntent)
            }
        }

        setContent {
            AntiTheftSystemTheme(
                darkTheme = ThemeManager.isDarkTheme.value
            ) {
                AppNavigation()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        
        // If remote lockdown is active, we try to bring the app back immediately
        val securityPrefs = SecurityPrefs(applicationContext)
        val isRemoteLocked = kotlinx.coroutines.runBlocking { 
            securityPrefs.isRemoteLockdownActive.first() 
        }
        
        if (isRemoteLocked) {
            android.util.Log.w("MainActivity", "Remote lockdown ACTIVE: Attempting to prevent app exit.")
            // Use a slight delay to allow the system to process the pause before re-foregrounding
            lifecycleScope.launch(Dispatchers.Main) {
                delay(100L)
                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
                startActivity(intent)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            val securityPrefs = SecurityPrefs(applicationContext)
            val isRemoteLocked = kotlinx.coroutines.runBlocking { 
                securityPrefs.isRemoteLockdownActive.first() 
            }
            
            if (isRemoteLocked) {
                android.util.Log.w("MainActivity", "Lockdown active and focus lost. Re-triggering app.")
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(100L)
                    val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val securityPrefs = remember { SecurityPrefs(context) }
    
    val failedUnlockCount = securityPrefs.failedUnlockCount.collectAsState(initial = 0).value
    val isRemoteLocked = securityPrefs.isRemoteLockdownActive.collectAsState(initial = false).value
    val isLostMode = securityPrefs.isLostModeActive.collectAsState(initial = false).value
    val lostMsg = securityPrefs.lostModeMessage.collectAsState(initial = "THIS DEVICE IS LOST").value
    val lostPhone = securityPrefs.lostModePhone.collectAsState(initial = "").value
    
    val riskScore = RiskEngine.calculateRiskScore(
        isTrustedDevice = true,
        isKnownLocation = true,
        failedUnlockAttempts = failedUnlockCount
    )
    val securityLevel = RiskEngine.getSecurityLevel(riskScore)
    
    var isManuallyUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(isRemoteLocked, isLostMode) {
        android.util.Log.d("MainActivity", "Lockdown: \$isRemoteLocked, LostMode: \$isLostMode")
        
        if (isRemoteLocked || isLostMode) {
            // Reset manual unlock state when a new lockdown is detected
            if (isManuallyUnlocked) {
                android.util.Log.i("MainActivity", "New lockdown/lost detected, resetting manual unlock.")
                isManuallyUnlocked = false
            }

            // Aggressive Pinning
            try {
                (context as? ComponentActivity)?.startLockTask()
                android.util.Log.i("MainActivity", "Lock Task STARTED")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start Lock Task: \${e.message}")
            }

            val target = if (isLostMode) "lost-mode" else "antitheft"
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            // Release Pinning
            try {
                (context as? ComponentActivity)?.stopLockTask()
                android.util.Log.i("MainActivity", "Lock Task STOPPED")
            } catch (e: Exception) { }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if ((securityLevel == SecurityLevel.CRITICAL || isRemoteLocked) && !isManuallyUnlocked) "antitheft" 
                           else if (isLostMode) "lost-mode"
                           else "login"
    ) {
        composable("lost-mode") {
            LostModeScreen(message = lostMsg, phoneNumber = lostPhone)
        }
        composable("antitheft") {
            AntiTheftLockScreen(
                onUnlockSuccess = { 
                    isManuallyUnlocked = true
                    try {
                        (context as? ComponentActivity)?.stopLockTask()
                    } catch (e: Exception) { }
                }
            )
        }

        composable("lockdown") {
            LockdownScreen(
                riskScore = riskScore,
                onContactSupport = { /* Action to contact support */ }
            )
        }

        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { username -> 
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToMFA = { username ->
                    navController.navigate("mfa/$username")
                },
                onNavigateToLockdown = {
                    navController.navigate("lockdown")
                },
                onNavigateToVerification = { username ->
                    navController.navigate("register-verify/$username")
                }
            )
        }
        
        composable("mfa/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            MFAScreen(
                username = username,
                onVerifySuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("register") {
            RegisterScreen(
                onBackToLogin = { navController.popBackStack() },
                onNavigateToVerification = { username ->
                    navController.navigate("register-verify/$username")
                }
            )
        }

        composable("register-verify/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            RegisterMFAScreen(
                username = username,
                onVerifySuccess = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToLocation = { navController.navigate("location") },
                onNavigateToDevices = { navController.navigate("devices") }
            )
        }

        composable("location?lat={lat}&lon={lon}") { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
            LocationTrackingScreen(
                onBack = { navController.popBackStack() },
                initialLat = lat,
                initialLon = lon
            )
        }

        composable("location") {
            LocationTrackingScreen(onBack = { navController.popBackStack() })
        }

        composable("devices") {
            DeviceManagementScreen(onBack = { navController.popBackStack() })
        }

        composable("history") {
            LocationHistoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMap = { lat, lon ->
                    navController.navigate("location?lat=$lat&lon=$lon")
                }
            )
        }

        composable("sessions") {
            SessionManagementScreen(onBack = { navController.popBackStack() })
        }
    }
}
