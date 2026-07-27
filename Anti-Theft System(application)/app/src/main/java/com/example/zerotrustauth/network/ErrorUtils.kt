package com.example.zerotrustauth.network

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

object ErrorUtils {
    fun parseErrorMessage(e: Throwable): String {
        return when (e) {
            is UnknownHostException, is ConnectException -> 
                "Không có kết nối internet. Vui lòng kiểm tra lại mạng."
            is SocketTimeoutException -> 
                "Kết nối máy chủ bị quá hạn. Vui lòng thử lại sau."
            is HttpException -> {
                when (e.code()) {
                    401 -> "Tên đăng nhập hoặc mật khẩu không chính xác."
                    403 -> "Tài khoản của bạn đã bị khoá hoặc không có quyền truy cập."
                    404 -> "Không tìm thấy tài khoản hoặc yêu cầu. Vui lòng kiểm tra lại thông tin."
                    429 -> "Bạn đã gửi quá nhiều yêu cầu. Vui lòng đợi một lát."
                    500, 502, 503, 504 -> "Lỗi máy chủ hệ thống. Chúng tôi đang khắc phục."
                    else -> "Lỗi mạng (${e.code()}). Vui lòng thử lại."
                }
            }
            else -> e.message ?: "Đã có lỗi không xác định xảy ra."
        }
    }
}
