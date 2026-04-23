package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserCreateRequest(
    @field:NotBlank(message = "用户名不能为空")
    @field:Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @field:Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Schema(description = "用户名（3-50字符，仅字母数字下划线）", example = "teacher2", required = true)
    val username: String = "",

    @field:NotBlank(message = "密码不能为空")
    @field:Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    @Schema(description = "密码（6-20字符）", example = "password123", required = true)
    val password: String = "",

    @field:Size(max = 50, message = "真实姓名长度不能超过50")
    @Schema(description = "真实姓名", example = "张三", required = false)
    val realName: String? = null,

    @field:NotBlank(message = "角色不能为空")
    @field:Pattern(regexp = "^(admin|teacher|student)$", message = "角色只能是admin、teacher或student")
    @Schema(description = "角色（admin/teacher/student）", example = "teacher", required = true, allowableValues = ["admin", "teacher", "student"])
    val role: String = "",

    @field:Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱（可选）", example = "teacher2@example.com", required = false)
    val email: String? = null
)
