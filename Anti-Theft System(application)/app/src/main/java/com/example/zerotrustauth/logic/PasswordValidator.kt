package com.example.zerotrustauth.logic

object PasswordValidator {
    /**
     * Validates if a password meets the strength requirements:
     * - At least 8 characters long
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     * - Contains at least one digit
     * - Contains at least one special character (@#$%^&+=! etc.)
     */
    fun validate(password: String): ValidationResult {
        if (password.length < 8) {
            return ValidationResult(false, "Mật khẩu phải có ít nhất 8 ký tự.")
        }
        if (!password.any { it.isUpperCase() }) {
            return ValidationResult(false, "Mật khẩu phải chứa ít nhất một chữ hoa.")
        }
        if (!password.any { it.isLowerCase() }) {
            return ValidationResult(false, "Mật khẩu phải chứa ít nhất một chữ thường.")
        }
        if (!password.any { it.isDigit() }) {
            return ValidationResult(false, "Mật khẩu phải chứa ít nhất một chữ số.")
        }
        val specialChars = "@#$%^&+=!"
        if (!password.any { specialChars.contains(it) }) {
            return ValidationResult(false, "Mật khẩu phải chứa ít nhất một ký tự đặc biệt ($specialChars).")
        }
        return ValidationResult(true)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )
}
