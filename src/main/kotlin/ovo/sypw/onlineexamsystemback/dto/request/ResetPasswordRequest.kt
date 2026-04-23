package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ResetPasswordRequest(
    @field:NotBlank(message = "新密码不能为空")
    @field:Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    @Schema(description = "新密码（6-20字符）", example = "newPassword123", required = true)
    val newPassword: String = ""
)
