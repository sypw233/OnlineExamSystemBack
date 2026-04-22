package ovo.sypw.onlineexamsystemback.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class UserProfileRequest(
    @field:Size(max = 50, message = "昵称长度不能超过50")
    val nickname: String? = null,

    @field:Email(message = "邮箱格式不正确")
    val email: String? = null,

    @field:Size(max = 500, message = "头像地址长度不能超过500")
    val avatar: String? = null
)
