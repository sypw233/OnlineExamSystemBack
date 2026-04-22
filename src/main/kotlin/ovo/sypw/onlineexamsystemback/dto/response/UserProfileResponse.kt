package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "当前登录用户信息")
data class UserProfileResponse(
    @Schema(description = "用户ID", example = "1")
    val id: Long,

    @Schema(description = "用户名", example = "admin")
    val username: String,

    @Schema(description = "昵称", example = "管理员")
    val nickname: String?,

    @Schema(description = "真实姓名", example = "系统管理员")
    val realName: String?,

    @Schema(description = "角色", example = "admin")
    val role: String,

    @Schema(description = "邮箱", example = "admin@example.com")
    val email: String?,

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    val avatar: String?
)
