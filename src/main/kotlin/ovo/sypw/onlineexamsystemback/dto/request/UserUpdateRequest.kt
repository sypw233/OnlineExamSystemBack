package ovo.sypw.onlineexamsystemback.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserUpdateRequest(
    @field:Size(max = 50, message = "真实姓名长度不能超过50")
    val realName: String? = null,

    @field:Email(message = "邮箱格式不正确")
    val email: String? = null,

    @field:Pattern(regexp = "^(admin|teacher|student)$", message = "角色只能是admin、teacher或student")
    val role: String? = null,

    // 0-禁用, 1-启用
    val status: Int? = null
)
