package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserResponse(
    @Schema(description = "用户ID", example = "1")
    val id: Long,

    @Schema(description = "用户名", example = "student1")
    val username: String,

    @Schema(description = "用户昵称", example = "小张")
    val nickname: String?,

    @Schema(description = "真实姓名", example = "张三")
    val realName: String?,

    @Schema(description = "角色", example = "student", allowableValues = ["admin", "teacher", "student"])
    val role: String,

    @Schema(description = "邮箱", example = "student1@example.com")
    val email: String?,

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    val avatar: String?,

    @Schema(description = "状态：1=启用，0=禁用", example = "1")
    val status: Int,

    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    val createTime: LocalDateTime
)
