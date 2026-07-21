package com.example.zerotrustauth.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerotrustauth.ThemeManager
import com.example.zerotrustauth.data.SecurityPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PinEntryScreen(
    onSuccess: (String) -> Unit,
    onFallback: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isDarkMode = ThemeManager.isDarkTheme.value
    val context = androidx.compose.ui.platform.LocalContext.current
    val securityPrefs = remember { SecurityPrefs(context) }
    val scope = rememberCoroutineScope()

    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🔐", fontSize = 64.sp)
            Text(
                "Nhập mã PIN",
                style = MaterialTheme.typography.headlineSmall,
                color = if (isDarkMode) Color.White else Color.Black
            )
            Text(
                "Mã PIN bảo mật phiên làm việc của bạn",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = pin,
                onValueChange = { 
                    if (it.length <= 4) {
                        pin = it
                        errorMessage = null
                        if (it.length == 4) {
                            scope.launch {
                                val storedPin = securityPrefs.localPin.first()
                                if (pin == storedPin) {
                                    val user = securityPrefs.username.first() ?: "guest"
                                    onSuccess(user)
                                } else {
                                    errorMessage = "Mã PIN không chính xác"
                                    pin = ""
                                }
                            }
                        }
                    }
                },
                label = { Text("Mã PIN 4 số") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.width(180.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onFallback) {
                Text("Sử dụng tài khoản khác / Đăng nhập lại", color = Color.Gray)
            }
        }
    }
}
