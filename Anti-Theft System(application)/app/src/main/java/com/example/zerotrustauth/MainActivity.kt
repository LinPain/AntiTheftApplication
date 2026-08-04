package com.example.zerotrustauth

import android.os.Bundle
import android.content.Context
import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zerotrustauth.ui.theme.AntiTheftSystemTheme
import com.example.zerotrustauth.ui.login.*
import com.example.zerotrustauth.ui.register.*
import com.example.zerotrustauth.ui.dashboard.*
import com.example.zerotrustauth.ui.location.*
import com.example.zerotrustauth.ui.lockdown.*
import com.example.zerotrustauth.ui.history.*
import com.example.zerotrustauth.ui.antitheft.*
import com.example.zerotrustauth.service.*
import com.example.zerotrustauth.logic.*
import com.example.zerotrustauth.data.SecurityPrefs
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import com.example.zerotrustauth.receiver.SecurityAdminReceiver

class MainActivity : FragmentActivity() {
    
    private var isAppVisible = mutableStateOf(false)
    private var currentPermissionToExplain = mutableStateOf<PermissionInfo?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        findNextMissingPermission()
    }

    private val requestBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        findNextMissingPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AntiTheftSystemTheme(darkTheme = ThemeManager.isDarkTheme.value) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                    
                    val permission = currentPermissionToExplain.value
                    if (permission != null && isAppVisible.value) {
                        PermissionRationaleDialog(permission)
                    }
                }
            }

            LaunchedEffect(isAppVisible.value) {
                if (isAppVisible.value) {
                    delay(1500) // Initial stability delay
                    findNextMissingPermission()
                }
            }
        }
    }

    private fun findNextMissingPermission() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, SecurityAdminReceiver::class.java)

        when {
            // 2. Overlay Permission
            !Settings.canDrawOverlays(this) -> {
                currentPermissionToExplain.value = PermissionInfo(
                    title = "Hiển thị trên ứng dụng khác",
                    description = "Cần thiết để hệ thống có thể hiển thị màn hình khoá chống trộm ngay lập tức khi phát hiện rủi ro.",
                    permissions = emptyArray(),
                    isOverlay = true
                )
            }

            // 3. Foreground Location
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                currentPermissionToExplain.value = PermissionInfo(
                    title = "Vị trí chính xác",
                    description = "Cần thiết để theo dõi thiết bị thời gian thực và thiết lập vùng an toàn.",
                    permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }

            // 5. Device Admin
            !dpm.isAdminActive(adminComponent) -> {
                currentPermissionToExplain.value = PermissionInfo(
                    title = "Bảo vệ mã PIN",
                    description = "Kích hoạt quyền Quản trị viên thiết bị để app có thể phát hiện khi kẻ trộm nhập sai mã PIN màn hình khoá.",
                    permissions = emptyArray(),
                    isDeviceAdmin = true
                )
            }

            // 6. Background Location
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                currentPermissionToExplain.value = PermissionInfo(
                    title = "Vị trí chạy ngầm",
                    description = "Quan trọng: Để bảo vệ thiết bị liên tục, vui lòng chọn 'Luôn cho phép' trong phần cài đặt vị trí.",
                    permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    isBackgroundLocation = true
                )
            }

            else -> {
                currentPermissionToExplain.value = null
                startSecurityServices()
            }
        }
    }

    @Composable
    private fun PermissionRationaleDialog(permission: PermissionInfo) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(permission.title, fontWeight = FontWeight.Bold) },
            text = { Text(permission.description) },
            confirmButton = {
                Button(onClick = {
                    currentPermissionToExplain.value = null // Hide dialog first
                    
                    lifecycleScope.launch {
                        delay(500) // UI settling delay
                        try {
                            when {
                                permission.isOverlay -> {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.fromParts("package", packageName, null)
                                        )
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback: Open general Overlay list
                                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                        Toast.makeText(this@MainActivity, "Vui lòng tìm và chọn App trong danh sách", Toast.LENGTH_LONG).show()
                                    }
                                }
                                permission.isDeviceAdmin -> {
                                    val adminComponent = ComponentName(this@MainActivity, SecurityAdminReceiver::class.java)
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, permission.description)
                                    }
                                    startActivity(intent)
                                }
                                permission.isBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback: Open general location settings
                                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                        Toast.makeText(this@MainActivity, "Vui lòng chọn 'Quyền' -> 'Vị trí' -> 'Luôn cho phép'", Toast.LENGTH_LONG).show()
                                    }
                                }
                                permission.isBackgroundLocation -> {
                                    requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                                else -> {
                                    requestPermissionLauncher.launch(permission.permissions)
                                }
                            }
                        } catch (e: Exception) {
                            // Ultimate fallback: App Info page
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            } catch (ex: Exception) {
                                Toast.makeText(this@MainActivity, "Vui lòng cấp quyền thủ công trong Cài đặt", Toast.LENGTH_LONG).show()
                            }
                            findNextMissingPermission()
                        }
                    }
                }) {
                    Text(if (permission.isBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || permission.isOverlay) "Mở Cài đặt" else "Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    currentPermissionToExplain.value = null
                    lifecycleScope.launch {
                        delay(1000) 
                        findNextMissingPermission()
                    }
                }) {
                    Text("Bỏ qua")
                }
            }
        )
    }

    data class PermissionInfo(
        val title: String,
        val description: String,
        val permissions: Array<String>,
        val isBackgroundLocation: Boolean = false,
        val isDeviceAdmin: Boolean = false,
        val isOverlay: Boolean = false
    )

    private fun startSecurityServices() {
        try {
            val alarmIntent = Intent(this, AlarmService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(alarmIntent)
            else startService(alarmIntent)

            val securityPrefs = SecurityPrefs(applicationContext)
            lifecycleScope.launch {
                if (securityPrefs.isLiveTrackingEnabled.first()) {
                    val locationIntent = Intent(this@MainActivity, LocationService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(locationIntent)
                    else startService(locationIntent)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Service start failed: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        isAppVisible.value = true
        AppState.isAppInForeground.value = true
        
        // Re-enforce lock task if we are in lost mode or lockdown
        val securityPrefs = SecurityPrefs(applicationContext)
        lifecycleScope.launch {
            val isRemoteLocked = securityPrefs.isRemoteLockdownActive.first()
            val isLost = securityPrefs.isLostModeActive.first()
            if (isRemoteLocked || isLost) {
                try {
                    // Force the window to stay on top even after reboots
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                                  android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                                  android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                                  android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

                    startLockTask()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to re-start lock task: ${e.message}")
                }
            }
        }

        // Re-check permissions when returning from settings or admin screens
        if (currentPermissionToExplain.value == null) {
            findNextMissingPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        isAppVisible.value = false
        AppState.isAppInForeground.value = false
        enforceLockdown()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) enforceLockdown()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val securityPrefs = SecurityPrefs(applicationContext)
        lifecycleScope.launch {
            val isRemoteLocked = securityPrefs.isRemoteLockdownActive.first()
            val isLost = securityPrefs.isLostModeActive.first()
            if (isRemoteLocked || isLost) {
                enforceLockdown()
            }
        }
    }

    private fun enforceLockdown() {
        val securityPrefs = SecurityPrefs(applicationContext)
        lifecycleScope.launch(Dispatchers.Main) {
            val isRemoteLocked = securityPrefs.isRemoteLockdownActive.first()
            val isLost = securityPrefs.isLostModeActive.first()

            if ((isRemoteLocked || isLost) && !isAppVisible.value) {
                delay(200L)
                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                startActivity(intent)
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
    val isRemoteLockedState = securityPrefs.isRemoteLockdownActive.collectAsState(initial = null)
    val isLostModeState = securityPrefs.isLostModeActive.collectAsState(initial = null)
    
    val isRemoteLocked = isRemoteLockedState.value
    val isLostMode = isLostModeState.value
    
    // Wait for initial values from DataStore to avoid race conditions/flicker
    if (isRemoteLocked == null || isLostMode == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val lostMsg = securityPrefs.lostModeMessage.collectAsState(initial = "LOST").value
    val lostPhone = securityPrefs.lostModePhone.collectAsState(initial = "").value
    val ownerName = securityPrefs.ownerName.collectAsState(initial = "").value
    val ownerPhone = securityPrefs.ownerPhone.collectAsState(initial = "").value
    val ownerEmail = securityPrefs.ownerEmail.collectAsState(initial = "").value

    val isOutsideSafeZone = securityPrefs.isOutsideSafeZone.collectAsState(initial = false).value
    val scope = rememberCoroutineScope()
    
    val riskScore = RiskEngine.calculateRiskScore(
        isTrustedDevice = true,
        isOutsideSafeZone = isOutsideSafeZone,
        failedUnlockAttempts = failedUnlockCount
    )
    val securityLevel = RiskEngine.getSecurityLevel(riskScore)
    
    var isManuallyUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(isRemoteLocked, isLostMode) {
        if (isRemoteLocked || isLostMode) {
            if (isManuallyUnlocked) isManuallyUnlocked = false
            
            try {
                (context as? ComponentActivity)?.startLockTask()
            } catch (e: Exception) { 
                android.util.Log.e("Navigation", "Pinning failed: ${e.message}")
            }

            val target = if (isLostMode) "lost-mode" else "antitheft"
            // Only navigate if we're not already on the target screen to avoid flickering
            if (navController.currentDestination?.route != target) {
                navController.navigate(target) { popUpTo(0) { inclusive = true } }
            }
        } else {
            try {
                (context as? ComponentActivity)?.stopLockTask()
            } catch (e: Exception) { }
            
            // If we were on a lock screen, navigate back to the secure entry (splash)
            val currentRoute = navController.currentDestination?.route
            if (currentRoute == "lost-mode" || currentRoute == "antitheft") {
                navController.navigate("splash") { popUpTo(0) { inclusive = true } }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            LaunchedEffect(isRemoteLocked, isLostMode) {
                // Determine destination as soon as data is ready
                if (isRemoteLocked == true || securityLevel == SecurityLevel.CRITICAL) {
                    navController.navigate("antitheft") { popUpTo(0) { inclusive = true } }
                } else if (isLostMode == true) {
                    navController.navigate("lost-mode") { popUpTo(0) { inclusive = true } }
                } else {
                    val isRemembered = securityPrefs.isRememberMeEnabled.first()
                    val hasPin = !securityPrefs.localPin.first().isNullOrBlank()
                    val hasToken = !securityPrefs.authToken.first().isNullOrBlank()
                    
                    val dest = if (isRemembered && hasPin && hasToken) "pin-entry" else "login"
                    navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                }
            }
            
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable("pin-entry") {
            PinEntryScreen(
                onSuccess = { u -> navController.navigate("dashboard") { popUpTo(0) { inclusive = true } } },
                onFallback = {
                    scope.launch {
                        securityPrefs.setRememberMe(false)
                        securityPrefs.setLocalPin(null)
                        securityPrefs.clearAuthData()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                }
            )
        }

        composable("lost-mode") { 
            LostModeScreen(
                message = lostMsg, 
                phoneNumber = if (lostPhone.isNotBlank()) lostPhone else (ownerPhone ?: ""),
                ownerName = ownerName ?: "",
                ownerEmail = ownerEmail ?: ""
            ) 
        }
        composable("antitheft") {
            AntiTheftLockScreen(onUnlockSuccess = { 
                isManuallyUnlocked = true
                try { (context as? ComponentActivity)?.stopLockTask() } catch (e: Exception) { }
                // Immediately navigate away upon successful manual unlock
                navController.navigate("splash") { popUpTo(0) { inclusive = true } }
            })
        }
        composable("lockdown") { LockdownScreen(riskScore, {}) }
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot-password") },
                onLoginSuccess = { u -> navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } },
                onNavigateToMFA = { u -> navController.navigate("mfa/$u") },
                onNavigateToLockdown = { navController.navigate("lockdown") },
                onNavigateToVerification = { u -> navController.navigate("register-verify/$u") }
            )
        }
        composable("forgot-password") {
            ForgotPasswordScreen(onSuccess = { navController.navigate("login") { popUpTo("login") { inclusive = true } } }, onBack = { navController.popBackStack() })
        }
        composable("mfa/{username}") { backStackEntry ->
            MFAScreen(backStackEntry.arguments?.getString("username") ?: "", { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } }, { navController.popBackStack() })
        }
        composable("register") {
            RegisterScreen({ navController.popBackStack() }, { u -> navController.navigate("register-verify/$u") })
        }
        composable("register-verify/{username}") { backStackEntry ->
            RegisterMFAScreen(backStackEntry.arguments?.getString("username") ?: "", { navController.navigate("login") { popUpTo(0) { inclusive = true } } }, { navController.popBackStack() })
        }
        composable("dashboard") {
            DashboardScreen({ navController.navigate("login") { popUpTo("dashboard") { inclusive = true } } }, { navController.navigate("history") }, { navController.navigate("location") }, { navController.navigate("devices") })
        }
        composable("location?lat={lat}&lon={lon}") { backStackEntry ->
            LocationTrackingScreen({ navController.popBackStack() }, backStackEntry.arguments?.getString("lat")?.toDoubleOrNull(), backStackEntry.arguments?.getString("lon")?.toDoubleOrNull())
        }
        composable("location") { LocationTrackingScreen({ navController.popBackStack() }) }
        composable("devices") { DeviceManagementScreen({ navController.popBackStack() }) }
        composable("history") { LocationHistoryScreen({ navController.popBackStack() }, { lat, lon -> navController.navigate("location?lat=$lat&lon=$lon") }) }
        composable("sessions") { SessionManagementScreen({ navController.popBackStack() }) }
    }
}
