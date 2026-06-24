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

import com.example.zerotrustauth.logic.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLocation: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDarkMode = ThemeManager.isDarkTheme.value
    val apiService = remember { LocationApiService.create() }
    var recentActivities by remember { mutableStateOf<List<LocationResponse>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (!PermissionHelper.hasOverlayPermission(context)) {
            PermissionHelper.requestOverlayPermission(context)
        }

        try {
            // Fetch only top 3 for the dashboard
            recentActivities = apiService.getLocationHistory("android_device_1").take(3)
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
                    IconButton(onClick = onLogout) {
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
                    SecurityStatusHeader(isDarkMode)
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
                                modifier = Modifier.weight(1.0f).clickable { /* MFA Info */ },
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
                                title = "Vị trí",
                                icon = Icons.Default.Map,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.weight(1.0f).clickable { onNavigateToLocation() },
                                isDarkMode = isDarkMode
                            )
                            Spacer(modifier = Modifier.weight(1.0f))
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
    }
}

@Composable
fun SecurityStatusHeader(isDarkMode: Boolean) {
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
                    .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Trạng thái: An toàn",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    "Risk Score: 15 (Rủi ro thấp)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF10B981)
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
