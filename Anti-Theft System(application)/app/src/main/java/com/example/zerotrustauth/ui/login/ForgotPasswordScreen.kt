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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerotrustauth.ThemeManager
import com.example.zerotrustauth.ui.theme.*
import com.example.zerotrustauth.network.*
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Email, 2: OTP, 3: New Password
    var identifier by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var usernameFromServer by remember { mutableStateOf("") }
    var resetToken by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val isDarkMode = ThemeManager.isDarkTheme.value
    val scope = rememberCoroutineScope()
    val apiService = remember { LocationApiService.create() }

    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(colors = listOf(Color(0xFF020617), Color(0xFF0F172A)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient).padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🔑", fontSize = 64.sp)
                    Text(
                        text = if (step == 3) "MẬT KHẨU MỚI" else "QUÊN MẬT KHẨU",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    when (step) {
                        1 -> {
                            Text("Nhập Email hoặc Tên đăng nhập để nhận mã OTP.", textAlign = TextAlign.Center, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = identifier,
                                onValueChange = { identifier = it; errorMessage = null },
                                label = { Text("Email hoặc Username") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        2 -> {
                            Text("Nhập mã OTP đã được gửi đến email của bạn.", textAlign = TextAlign.Center, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { if (it.length <= 6) otpCode = it; errorMessage = null },
                                label = { Text("Mã OTP (6 số)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        3 -> {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it; errorMessage = null },
                                label = { Text("Mật khẩu mới") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = null },
                                label = { Text("Xác nhận mật khẩu") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            isLoading = true
                            scope.launch {
                                try {
                                    when (step) {
                                        1 -> {
                                            if (identifier.isBlank()) throw Exception("Vui lòng nhập Email hoặc Username")
                                            val cleanId = identifier.trim().lowercase()
                                            
                                            val resp = apiService.forgotPassword(ForgotPasswordRequest(cleanId))
                                            // Fallback to identifier if username is not returned
                                            usernameFromServer = resp.username ?: cleanId
                                            step = 2
                                            errorMessage = null
                                        }
                                        2 -> {
                                            if (otpCode.length < 6) throw Exception("Vui lòng nhập đủ 6 số OTP")
                                            val resp = apiService.verifyReset(VerifyResetRequest(usernameFromServer, otpCode))
                                            resetToken = resp.resetToken ?: throw Exception("Không nhận được mã xác thực đặt lại")
                                            step = 3
                                            errorMessage = null
                                        }
                                        3 -> {
                                            if (newPassword != confirmPassword) throw Exception("Mật khẩu không khớp")
                                            val validation = com.example.zerotrustauth.logic.PasswordValidator.validate(newPassword)
                                            if (!validation.isValid) {
                                                errorMessage = validation.errorMessage
                                            } else {
                                                apiService.resetPassword(ResetPasswordRequest(resetToken, newPassword))
                                                errorMessage = null
                                                onSuccess()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = com.example.zerotrustauth.network.ErrorUtils.parseErrorMessage(e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text(if (step == 3) "ĐẶT LẠI" else "TIẾP TỤC")
                    }

                    TextButton(onClick = onBack) {
                        Text("Quay lại", color = if (isDarkMode) Color.Cyan else Color.Gray)
                    }
                }
            }
        }
    }
}
