package com.example.zerotrustauth

import android.os.Bundle
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.net.Uri
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

class MainActivity : FragmentActivity() {
    
    private var isAppVisible = false
    private val showPermissionRationale = mutableStateOf(false)
    private val currentPermissionIndex = mutableIntStateOf(0)

    private val permissionsToRequest = mutableListOf<PermissionInfo>()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Use a local copy of index to avoid race conditions if needed, 
        // but here we just increment and check bounds safely.
        val nextIndex = currentPermissionIndex.intValue + 1
        if (nextIndex < permissionsToRequest.size) {
            currentPermissionIndex.intValue = nextIndex
            showPermissionRationale.value = true
        } else {
            // Finished current batch
            if (permissions.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    checkBackgroundLocation()
                } else {
                    startSecurityServices()
                }
            } else {
                startSecurityServices()
            }
        }
    }

    private val requestBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        setContent {
            AntiTheftSystemTheme(darkTheme = ThemeManager.isDarkTheme.value) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                    if (showPermissionRationale.value) {
                        PermissionRationaleDialog()
                    }
                }
            }

            LaunchedEffect(Unit) {
                checkPermissions()
            }
        }
    }

    private fun checkPermissions() {
        val list = mutableListOf<PermissionInfo>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            list.add(PermissionInfo(
                title = "Vị trí chính xác",
                description = "Cần thiết để theo dõi thiết bị thời gian thực và thiết lập vùng an toàn (Geofencing).",
                permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            ))
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            list.add(PermissionInfo(
                title = "Máy ảnh",
                description = "Sử dụng để chụp ảnh kẻ xâm nhập khi có nỗ lực mở khoá trái phép.",
                permissions = arrayOf(Manifest.permission.CAMERA)
            ))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                list.add(PermissionInfo(
                    title = "Thông báo",
                    description = "Để gửi cảnh báo bảo mật khẩn cấp và trạng thái hệ thống cho bạn.",
                    permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                ))
            }
        }

        if (list.isNotEmpty()) {
            permissionsToRequest.clear()
            permissionsToRequest.addAll(list)
            currentPermissionIndex.intValue = 0
            showPermissionRationale.value = true
        } else {
            // All initial permissions granted, check background location if needed
            checkBackgroundLocation()
            startSecurityServices()
        }
    }

    private fun checkBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            // This is usually requested separately after fine location
            permissionsToRequest.clear()
            permissionsToRequest.add(PermissionInfo(
                title = "Vị trí chạy ngầm",
                description = "Cho phép ứng dụng bảo vệ thiết bị ngay cả khi bạn không mở ứng dụng.",
                permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            ))
            currentPermissionIndex.intValue = 0
            showPermissionRationale.value = true
        }
    }

    @Composable
    private fun PermissionRationaleDialog() {
        val index = currentPermissionIndex.intValue
        if (index < 0 || index >= permissionsToRequest.size) {
            showPermissionRationale.value = false
            return
        }
        val current = permissionsToRequest[index]

        AlertDialog(
            onDismissRequest = { showPermissionRationale.value = false },
            title = { Text(current.title, fontWeight = FontWeight.Bold) },
            text = { Text(current.description) },
            confirmButton = {
                Button(onClick = {
                    showPermissionRationale.value = false
                    requestPermissionLauncher.launch(current.permissions)
                }) {
                    Text("Đã hiểu & Cấp quyền")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale.value = false }) {
                    Text("Để sau")
                }
            }
        )
    }

    data class PermissionInfo(
        val title: String,
        val description: String,
        val permissions: Array<String>
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
        isAppVisible = true
    }

    override fun onPause() {
        super.onPause()
        isAppVisible = false
        enforceLockdown()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) enforceLockdown()
    }

    private fun enforceLockdown() {
        val securityPrefs = SecurityPrefs(applicationContext)
        lifecycleScope.launch(Dispatchers.Main) {
            val isRemoteLocked = securityPrefs.isRemoteLockdownActive.first()
            val isLost = securityPrefs.isLostModeActive.first()

            if ((isRemoteLocked || isLost) && !isAppVisible) {
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
    val isRemoteLocked = securityPrefs.isRemoteLockdownActive.collectAsState(initial = false).value
    val isLostMode = securityPrefs.isLostModeActive.collectAsState(initial = false).value
    val lostMsg = securityPrefs.lostModeMessage.collectAsState(initial = "LOST").value
    val lostPhone = securityPrefs.lostModePhone.collectAsState(initial = "").value
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
            navController.navigate(target) { popUpTo(0) { inclusive = true } }
        } else {
            try {
                (context as? ComponentActivity)?.stopLockTask()
            } catch (e: Exception) { }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if ((securityLevel == SecurityLevel.CRITICAL || isRemoteLocked) && !isManuallyUnlocked) "antitheft" 
                           else if (isLostMode) "lost-mode"
                           else "splash"
    ) {
        composable("splash") {
            var startDest by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                val isRemembered = securityPrefs.isRememberMeEnabled.first()
                val hasPin = !securityPrefs.localPin.first().isNullOrBlank()
                val hasToken = !securityPrefs.authToken.first().isNullOrBlank()
                
                startDest = if (isRemembered && hasPin && hasToken) "pin-entry" else "login"
            }
            
            startDest?.let {
                navController.navigate(it) { popUpTo("splash") { inclusive = true } }
            } ?: Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
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

        composable("lost-mode") { LostModeScreen(lostMsg, lostPhone) }
        composable("antitheft") {
            AntiTheftLockScreen(onUnlockSuccess = { 
                isManuallyUnlocked = true
                try { (context as? ComponentActivity)?.stopLockTask() } catch (e: Exception) { }
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
