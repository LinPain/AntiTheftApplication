package com.example.zerotrustauth.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerotrustauth.ThemeManager
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.logic.LocationHelper
import com.example.zerotrustauth.service.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val securityPrefs = remember { SecurityPrefs(context) }
    val isDeviceTrusted by securityPrefs.isDeviceTrusted.collectAsState(initial = false)
    val authToken = securityPrefs.authToken.collectAsState(initial = null).value
    val username = securityPrefs.username.collectAsState(initial = "").value ?: ""
    val scope = rememberCoroutineScope()
    
    val locationHelper = remember { LocationHelper(context) }
    val currentDeviceId = remember { locationHelper.getDeviceId() }
    val apiService = remember(authToken) { com.example.zerotrustauth.network.LocationApiService.create(authToken) }
    
    var devices by remember { mutableStateOf<List<com.example.zerotrustauth.network.DeviceStatusResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            try {
                devices = apiService.getDeviceList(username)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val isDarkMode = ThemeManager.isDarkTheme.value
    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(colors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("THIẾT BỊ TIN CẬY", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(backgroundGradient).padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Quản lý trạng thái tin cậy của thiết bị này. Thiết bị không tin cậy sẽ tăng điểm rủi ro bảo mật.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                item {
                    TrustStatusCard(isDeviceTrusted, isDarkMode) { trusted ->
                        scope.launch {
                            securityPrefs.setDeviceTrusted(trusted)
                            // Trigger immediate pulse to sync device name and location to dashboard
                            if (trusted) {
                                LocationService.triggerImmediateUpload(context)
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Danh sách thiết bị",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (devices.isEmpty()) {
                    item {
                        Text("Chưa có thiết bị nào được ghi nhận.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    items(devices) { device ->
                        val isCurrent = device._id == currentDeviceId
                        DeviceRowImproved(
                            DeviceItemData(
                                name = device.deviceName ?: device._id,
                                info = if (isCurrent) "Đang hoạt động (Thiết bị này)" else "Lần cuối: ${device.lastTimestamp.split("T")[0]}",
                                isCurrent = isCurrent,
                                isTrusted = if (isCurrent) isDeviceTrusted else false // Note: Backend doesn't store trust yet
                            ),
                            isDarkMode = isDarkMode,
                            onDelete = if (!isCurrent) {
                                {
                                    scope.launch {
                                        try {
                                            apiService.removeDevice(username, device._id)
                                            devices = apiService.getDeviceList(username)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrustStatusCard(isTrusted: Boolean, isDarkMode: Boolean, onToggleTrust: (Boolean) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isTrusted) Icons.Default.VerifiedUser else Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = if (isTrusted) Color(0xFF10B981) else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isTrusted) "Thiết bị tin cậy" else "Thiết bị chưa tin cậy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Text(
                        text = if (isTrusted) "Thiết bị này được xác nhận là của bạn." else "Điểm rủi ro sẽ tăng 40 điểm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { onToggleTrust(!isTrusted) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTrusted) Color(0xFFEF4444) else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isTrusted) "GỠ TIN CẬY" else "TIN CẬY THIẾT BỊ NÀY")
            }
        }
    }
}

data class DeviceItemData(val name: String, val info: String, val isCurrent: Boolean, val isTrusted: Boolean)

@Composable
fun DeviceRowImproved(device: DeviceItemData, isDarkMode: Boolean, onDelete: (() -> Unit)? = null) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(
                    (if (device.isTrusted) Color(0xFF10B981) else Color.Gray).copy(alpha = 0.1f), 
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (device.name.contains("Web")) Icons.Default.Laptop else Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = if (device.isTrusted) Color(0xFF10B981) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(device.info, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            if (device.isTrusted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Trusted", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
            }
            
            onDelete?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}
