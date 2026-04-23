package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "创建/更新课程请求")
data class CourseRequest(
    @field:NotBlank(message = "课程名称不能为空")
    @field:Size(max = 100, message = "课程名称长度不能超过100")
    @Schema(description = "课程名称", example = "Java基础", required = true)
    val courseName: String = "",

    @field:Size(max = 500, message = "课程描述长度不能超过500")
    @Schema(description = "课程描述", example = "Java语言基础课程", required = false)
    val description: String? = null,

    @Schema(description = "状态：1=活跃, 0=停用", example = "1", defaultValue = "1")
    val status: Int = 1  // 1=active, 0=inactive
)
