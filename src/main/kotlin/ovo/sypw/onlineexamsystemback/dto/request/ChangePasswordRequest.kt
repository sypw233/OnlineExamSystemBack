package ovo.sypw.onlineexamsystemback.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "旧密码不能为空")
    val oldPassword: String = "",
    @field:NotBlank(message = "新密码不能为空")
    @field:Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    val newPassword: String = ""
)
