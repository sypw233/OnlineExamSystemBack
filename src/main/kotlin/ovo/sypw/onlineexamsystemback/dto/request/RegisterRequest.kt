package ovo.sypw.onlineexamsystemback.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "用户名不能为空")
    @field:Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @field:Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    val username: String = "",

    @field:NotBlank(message = "密码不能为空")
    @field:Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    val password: String = "",

    @field:NotBlank(message = "真实姓名不能为空")
    @field:Size(max = 50, message = "真实姓名长度不能超过50")
    val realName: String = "",

    @field:NotBlank(message = "角色不能为空")
    @field:Pattern(regexp = "^(student|teacher)$", message = "角色只能是student或teacher")
    val role: String = "",

    @field:Email(message = "邮箱格式不正确")
    val email: String? = null
)
