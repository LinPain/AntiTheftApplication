package com.example.zerotrustauth.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zerotrustauth.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginHistoryScreen(onBack: () -> Unit) {
    val isDarkMode = ThemeManager.isDarkTheme.value
    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(colors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LỊCH SỬ ĐĂNG NHẬP", fontWeight = FontWeight.Bold) },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                val historyList = listOf(
                    HistoryItem("Đăng nhập thành công", "12/10/2023 10:30", "Hà Nội, VN", "Chrome / Windows", Color(0xFF10B981)),
                    HistoryItem("Xác thực MFA thành công", "12/10/2023 10:31", "Hà Nội, VN", "App Mobile", Color(0xFF3B82F6)),
                    HistoryItem("Phát hiện đăng nhập lạ", "11/10/2023 23:15", "Singapore", "Safari / iOS", Color(0xFFEF4444)),
                    HistoryItem("Đổi mật khẩu", "10/10/2023 08:00", "Hà Nội, VN", "Chrome / macOS", Color(0xFFF59E0B))
                )

                items(historyList) { item ->
                    HistoryRow(item, isDarkMode)
                }
            }
        }
    }
}

data class HistoryItem(val action: String, val time: String, val location: String, val device: String, val statusColor: Color)

@Composable
fun HistoryRow(item: HistoryItem, isDarkMode: Boolean) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(item.statusColor, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.action, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(item.time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(item.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                "Thiết bị: ${item.device}", 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
