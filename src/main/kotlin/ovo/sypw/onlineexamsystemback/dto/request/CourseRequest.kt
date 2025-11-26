package ovo.sypw.onlineexamsystemback.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CourseRequest(
    @field:NotBlank(message = "课程名称不能为空")
    @field:Size(max = 100, message = "课程名称长度不能超过100")
    val courseName: String = "",

    @field:Size(max = 500, message = "课程描述长度不能超过500")
    val description: String? = null,

    val status: Int = 1  // 1=active, 0=inactive
)
