package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserUpdateRequest(
    @field:Size(max = 50, message = "用户名长度不能超过50")
    @Schema(description = "用户名", example = "zhangsan", required = false)
    val username: String? = null,

    @field:Size(max = 50, message = "真实姓名长度不能超过50")
    @Schema(description = "真实姓名", example = "张三", required = false)
    val realName: String? = null,

    @field:Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "zhangsan@example.com", required = false)
    val email: String? = null,

    @field:Pattern(regexp = "^(admin|teacher|student)$", message = "角色只能是admin、teacher或student")
    @Schema(description = "用户角色", example = "student", required = false, allowableValues = ["admin", "teacher", "student"])
    val role: String? = null,

    @Schema(description = "账号状态：1=启用，0=禁用", example = "1", required = false)
    val status: Int? = null
)
