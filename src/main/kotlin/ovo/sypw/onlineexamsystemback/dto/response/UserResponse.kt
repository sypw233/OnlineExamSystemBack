package ovo.sypw.onlineexamsystemback.dto.response

import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val realName: String?,
    val role: String,
    val email: String?,
    val status: Int,
    val createTime: LocalDateTime
)
