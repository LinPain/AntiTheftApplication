package com.example.zerotrustauth.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerotrustauth.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(onBack: () -> Unit) {
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
                        "Các thiết bị đang có quyền truy cập vào tài khoản của bạn.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                val devices = listOf(
                    DeviceItem("Samsung Galaxy S23 (Hiện tại)", "Việt Nam • Đang hoạt động", true),
                    DeviceItem("MacBook Pro 14\"", "Singapore • 2 giờ trước", false),
                    DeviceItem("iPhone 15 Pro", "Mỹ • 3 ngày trước", false)
                )

                items(devices) { device ->
                    DeviceRow(device, isDarkMode)
                }
            }
        }
    }
}

data class DeviceItem(val name: String, val info: String, val isCurrent: Boolean)

@Composable
fun DeviceRow(device: DeviceItem, isDarkMode: Boolean) {
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
                    (if (device.isCurrent) Color(0xFF10B981) else Color.Gray).copy(alpha = 0.1f), 
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (device.name.contains("MacBook")) Icons.Default.Laptop else Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = if (device.isCurrent) Color(0xFF10B981) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(device.info, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (!device.isCurrent) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}
