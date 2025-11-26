package ovo.sypw.onlineexamsystemback.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

object PasswordEncoderUtil {
    private val encoder = BCryptPasswordEncoder()

    @JvmStatic
    fun main(args: Array<String>) {
        // 生成一些常用测试密码的BCrypt哈希
        println("=== BCrypt密码生成器 ===")
        println()
        
        val passwords = listOf("admin123", "123456", "password", "test123")
        
        passwords.forEach { password ->
            val encoded = encoder.encode(password)
            println("原密码: $password")
            println("BCrypt: $encoded")
            println()
        }
    }
}
