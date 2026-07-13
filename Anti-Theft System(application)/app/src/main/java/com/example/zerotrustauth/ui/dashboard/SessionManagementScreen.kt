package com.example.zerotrustauth.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Token
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
fun SessionManagementScreen(onBack: () -> Unit) {
    val isDarkMode = ThemeManager.isDarkTheme.value
    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(colors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QUẢN LÝ PHIÊN", fontWeight = FontWeight.Bold) },
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
                        "Quản lý các Token JWT và phiên làm việc đang hoạt động.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                val sessions = listOf(
                    SessionItem("JWT Access Token", "Hết hạn trong: 59 phút", "Bảo mật mức cao", true),
                    SessionItem("Refresh Token", "Hết hạn trong: 30 ngày", "Dùng để gia hạn", false)
                )

                items(sessions) { session ->
                    SessionRow(session, isDarkMode)
                }
            }
        }
    }
}

data class SessionItem(val type: String, val expiry: String, val status: String, val isActive: Boolean)

@Composable
fun SessionRow(session: SessionItem, isDarkMode: Boolean) {
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
            Icon(Icons.Default.Token, contentDescription = null, tint = Color(0xFF8B5CF6))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.type, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(session.expiry, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (session.isActive) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = "Revoke", tint = Color.Red)
                }
            }
        }
    }
}
