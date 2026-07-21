package com.example.zerotrustauth.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.zerotrustauth.ThemeManager
import com.example.zerotrustauth.ui.theme.*
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.RegisterRequest
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    onNavigateToVerification: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var isRegistering by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val isDarkMode = ThemeManager.isDarkTheme.value
    val scope = rememberCoroutineScope()
    val apiService = remember { LocationApiService.create() }

    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onBackToLogin()
                }) { Text("Đăng nhập ngay") }
            },
            title = { Text("Đăng ký thành công!") },
            text = { Text("Tài khoản của bạn đã được bảo vệ bởi hệ thống Zero Trust. Vui lòng đăng nhập để bắt đầu.") }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            // Back Button
            IconButton(
                onClick = onBackToLogin,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDarkMode) Color.White else Color.Black
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🛡️", fontSize = 56.sp)
                        Text(
                            text = "TẠO TÀI KHOẢN",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (errorMessage != null) {
                            Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; errorMessage = null },
                            label = { Text("Tên đăng nhập") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isRegistering
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isRegistering
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Mật khẩu") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isRegistering
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            label = { Text("Xác nhận mật khẩu") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isRegistering
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                    errorMessage = "Vui lòng nhập đầy đủ thông tin"
                                } else if (password != confirmPassword) {
                                    errorMessage = "Mật khẩu không khớp"
                                } else {
                                    isRegistering = true
                                    scope.launch {
                                        try {
                                            val response = apiService.register(
                                                RegisterRequest(
                                                    username.trim().lowercase(), 
                                                    email.trim().lowercase(), 
                                                    password
                                                )
                                            )
                                            isRegistering = false
                                            if (response.verificationRequired) {
                                                onNavigateToVerification(username)
                                            } else {
                                                showSuccessDialog = true
                                            }
                                        } catch (e: Exception) {
                                            isRegistering = false
                                            errorMessage = "Đăng ký lỗi: ${e.message}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isRegistering,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF3B82F6) else NavyPrimary
                            )
                        ) {
                            if (isRegistering) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("ĐĂNG KÝ NGAY", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        TextButton(onClick = onBackToLogin, enabled = !isRegistering) {
                            Text("Đã có tài khoản? Đăng nhập", color = if (isDarkMode) Color.Cyan else NavyPrimary)
                        }
                    }
                }
            }
        }
    }
}
