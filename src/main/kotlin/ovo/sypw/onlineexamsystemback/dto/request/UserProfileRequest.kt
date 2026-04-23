package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class UserProfileRequest(
    @field:Size(max = 50, message = "昵称长度不能超过50")
    @Schema(description = "用户昵称", example = "小张", required = false)
    val nickname: String? = null,

    @field:Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "zhangsan@example.com", required = false)
    val email: String? = null,

    @field:Size(max = 500, message = "头像地址长度不能超过500")
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg", required = false)
    val avatar: String? = null
)
