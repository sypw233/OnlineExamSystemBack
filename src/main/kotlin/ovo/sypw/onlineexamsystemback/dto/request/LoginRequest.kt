package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "用户名不能为空")
    @field:Schema(description = "用户名", example = "student1", required = true)
    val username: String = "",

    @field:NotBlank(message = "密码不能为空")
    @field:Schema(description = "密码", example = "123456", required = true)
    val password: String = ""
)
