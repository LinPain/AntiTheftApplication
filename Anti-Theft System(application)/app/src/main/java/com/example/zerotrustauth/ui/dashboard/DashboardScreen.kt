package com.example.zerotrustauth.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerotrustauth.ThemeManager
import com.example.zerotrustauth.ui.theme.*
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LocationResponse
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import androidx.compose.ui.platform.LocalUriHandler
import com.example.zerotrustauth.data.SecurityPrefs
import androidx.compose.runtime.collectAsState
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.zerotrustauth.receiver.SecurityAdminReceiver
import com.example.zerotrustauth.logic.RiskEngine
import com.example.zerotrustauth.logic.SecurityLevel
import com.example.zerotrustauth.logic.PermissionHelper
import com.example.zerotrustauth.logic.LocationHelper
import com.example.zerotrustauth.ui.login.PinSetupDialog
import com.example.zerotrustauth.service.LocationService
import com.example.zerotrustauth.service.AlarmService
import android.os.Build
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToDevices: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = LocalUriHandler.current
    val securityPrefs = remember { SecurityPrefs(context) }
    val username = securityPrefs.username.collectAsState(initial = "guest").value ?: "guest"
    val authToken = securityPrefs.authToken.collectAsState(initial = null).value
    val failedUnlockCount = securityPrefs.failedUnlockCount.collectAsState(initial = 0).value
    val isDeviceTrusted = securityPrefs.isDeviceTrusted.collectAsState(initial = false).value
    val isOutsideSafeZone = securityPrefs.isOutsideSafeZone.collectAsState(initial = false).value
    val scope = rememberCoroutineScope()

    val riskScore = RiskEngine.calculateRiskScore(
        isTrustedDevice = isDeviceTrusted,
        isOutsideSafeZone = isOutsideSafeZone,
        failedUnlockAttempts = failedUnlockCount
    )
    val securityLevel = RiskEngine.getSecurityLevel(riskScore)
    
    val isDarkMode = ThemeManager.isDarkTheme.value
    val apiService = remember(authToken) { LocationApiService.create(authToken) }
    var recentActivities by remember { mutableStateOf<List<LocationResponse>>(emptyList()) }
    var showPinChangeDialog by remember { mutableStateOf(false) }

    // Device Admin Status
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, SecurityAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    LaunchedEffect(Unit) {
        isAdminActive = dpm.isAdminActive(adminComponent)
    }

    LaunchedEffect(username) {
        if (!PermissionHelper.hasOverlayPermission(context)) {
            PermissionHelper.requestOverlayPermission(context)
        }

        // Auto-enable tracking when logged in to dashboard
        scope.launch {
            if (!LocationService.isRunning) {
                securityPrefs.setLiveTracking(true)
                val intent = Intent(context, LocationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        try {
            val locationHelper = LocationHelper(context)
            recentActivities = apiService.getLocationHistory(username, locationHelper.getDeviceId()).take(3)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "TRANG CHỦ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            securityPrefs.setLiveTracking(false)
                            context.stopService(Intent(context, LocationService::class.java))
                            context.stopService(Intent(context, AlarmService::class.java))
                            securityPrefs.setRememberMe(false)
                            securityPrefs.setLocalPin(null)
                            securityPrefs.clearAuthData()
                            onLogout()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = if (isDarkMode) Color.White else Color.Black,
                    actionIconContentColor = if (isDarkMode) Color.White else Color.Black
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SecurityStatusHeader(isDarkMode, riskScore, securityLevel)
                }

                if (!isAdminActive) {
                    item {
                        DeviceAdminPrompt(isDarkMode) {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Cần quyền Admin để phát hiện kẻ trộm nhập sai mã PIN.")
                            }
                            context.startActivity(intent)
                        }
                    }
                }

                item {
                    Text(
                        "Dịch vụ bảo mật",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkMode) Color.White else Color.Black,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ServiceCard(
                                title = "Xác thực",
                                icon = Icons.Default.Fingerprint,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1.0f).clickable { showPinChangeDialog = true },
                                isDarkMode = isDarkMode
                            )
                            ServiceCard(
                                title = "Lịch sử",
                                icon = Icons.Default.History,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1.0f).clickable { onNavigateToHistory() },
                                isDarkMode = isDarkMode
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ServiceCard(
                                title = "Thiết bị",
                                icon = Icons.Default.Smartphone,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1.0f).clickable { onNavigateToDevices() },
                                isDarkMode = isDarkMode
                            )
                            ServiceCard(
                                title = "Vùng an toàn",
                                icon = Icons.Default.LocationSearching,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1.0f).clickable {
                                    scope.launch {
                                        recentActivities.firstOrNull()?.let {
                                            securityPrefs.setSafeZone(it.latitude, it.longitude)
                                            android.widget.Toast.makeText(context, "Đã đặt vị trí hiện tại làm Vùng an toàn!", android.widget.Toast.LENGTH_SHORT).show()
                                        } ?: run {
                                            android.widget.Toast.makeText(context, "Đang chờ vị trí...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Hoạt động gần đây",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkMode) Color.White else Color.Black,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (recentActivities.isEmpty()) {
                    item {
                        Text(
                            "Không có hoạt động mới",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                } else {
                    items(recentActivities) { activity ->
                        ActivityRowFromLocation(activity, isDarkMode, onClick = onNavigateToHistory)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showPinChangeDialog) {
            PinSetupDialog(
                title = "Đổi mã PIN bảo mật",
                onDismiss = { showPinChangeDialog = false },
                onPinSet = { newPin ->
                    scope.launch {
                        securityPrefs.setLocalPin(newPin)
                        showPinChangeDialog = false
                        android.widget.Toast.makeText(context, "Đã cập nhật mã PIN thành công!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun DeviceAdminPrompt(isDarkMode: Boolean, onActivate: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkMode) Color(0xFFB91C1C).copy(alpha = 0.2f) else Color(0xFFFEE2E2)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Kích hoạt bảo vệ PIN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    "Cho phép app phát hiện khi có người nhập sai mã PIN điện thoại.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) Color.LightGray else Color.DarkGray
                )
            }
            TextButton(onClick = onActivate) {
                Text("BẬT", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun SecurityStatusHeader(isDarkMode: Boolean, riskScore: Int, securityLevel: SecurityLevel) {
    val statusColor = when (securityLevel) {
        SecurityLevel.LOW -> Color(0xFF10B981)
        SecurityLevel.MEDIUM -> Color(0xFFF59E0B)
        SecurityLevel.HIGH -> Color(0xFFEF4444)
        SecurityLevel.CRITICAL -> Color(0xFFB91C1C)
    }

    val statusText = when (securityLevel) {
        SecurityLevel.LOW -> "An toàn"
        SecurityLevel.MEDIUM -> "Trung bình"
        SecurityLevel.HIGH -> "Rủi ro cao"
        SecurityLevel.CRITICAL -> "Nguy hiểm"
    }

    val riskDesc = when (securityLevel) {
        SecurityLevel.LOW -> "Rủi ro thấp"
        SecurityLevel.MEDIUM -> "Cần xác thực thêm"
        SecurityLevel.HIGH -> "Yêu cầu sinh trắc học"
        SecurityLevel.CRITICAL -> "Thiết bị bị khoá"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.9f) else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(statusColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (securityLevel) {
                        SecurityLevel.LOW -> Icons.Default.VerifiedUser
                        SecurityLevel.MEDIUM -> Icons.Default.Shield
                        SecurityLevel.HIGH -> Icons.Default.Warning
                        SecurityLevel.CRITICAL -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Trạng thái: $statusText",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    "Risk Score: $riskScore ($riskDesc)",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun ServiceCard(title: String, icon: ImageVector, color: Color, modifier: Modifier, isDarkMode: Boolean) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.9f) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isDarkMode) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun ActivityRowFromLocation(location: LocationResponse, isDarkMode: Boolean, onClick: () -> Unit) {
    val formattedTime = try {
        val zonedDateTime = ZonedDateTime.parse(location.timestamp)
        zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm, dd/MM"))
    } catch (e: Exception) {
        location.timestamp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Cập nhật vị trí mới",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) Color.White else Color.Black
            )
            Text(
                "${location.latitude}, ${location.longitude}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Text(
            formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}
