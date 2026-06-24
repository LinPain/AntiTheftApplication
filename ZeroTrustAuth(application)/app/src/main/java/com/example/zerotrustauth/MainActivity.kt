package com.example.zerotrustauth

import android.os.Bundle
import android.os.Build
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zerotrustauth.ui.theme.ZeroTrustAuthTheme
import com.example.zerotrustauth.ui.login.LoginScreen
import com.example.zerotrustauth.ui.login.MFAScreen
import com.example.zerotrustauth.ui.register.RegisterScreen
import com.example.zerotrustauth.ui.dashboard.*
import com.example.zerotrustauth.ui.location.LocationTrackingScreen
import com.example.zerotrustauth.ui.lockdown.LockdownScreen
import com.example.zerotrustauth.ui.history.LocationHistoryScreen
import com.example.zerotrustauth.ui.antitheft.AntiTheftLockScreen
import com.example.zerotrustauth.service.AlarmService
import com.example.zerotrustauth.logic.RiskEngine
import com.example.zerotrustauth.logic.SecurityLevel
import androidx.compose.runtime.collectAsState
import com.example.zerotrustauth.data.SecurityPrefs
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start the remote control listener service
        val alarmIntent = Intent(this, AlarmService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(alarmIntent)
        } else {
            startService(alarmIntent)
        }

        setContent {
            ZeroTrustAuthTheme(
                darkTheme = ThemeManager.isDarkTheme.value
            ) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val securityPrefs = remember { SecurityPrefs(context) }
    
    // In a real app, these values would come from the RiskEngine observing the system
    // For this demonstration, we'll collect the failed unlock count and SIM change flag
    val failedUnlockCount = securityPrefs.failedUnlockCount.collectAsState(initial = 0).value
    val isRemoteLocked = securityPrefs.isRemoteLockdownActive.collectAsState(initial = false).value
    
    // Simulate other factors for the demonstration
    val riskScore = RiskEngine.calculateRiskScore(
        isTrustedDevice = true,
        isKnownLocation = true,
        failedUnlockAttempts = failedUnlockCount
    )
    val securityLevel = RiskEngine.getSecurityLevel(riskScore)
    
    var isManuallyUnlocked by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = if ((securityLevel == SecurityLevel.CRITICAL || isRemoteLocked) && !isManuallyUnlocked) "antitheft" else "login"
    ) {
        composable("antitheft") {
            AntiTheftLockScreen(
                onUnlockSuccess = { isManuallyUnlocked = true }
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
                    navController.navigate("mfa/$username") 
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
                onBackToLogin = { navController.popBackStack() }
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
                onNavigateToLocation = { navController.navigate("location") }
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
