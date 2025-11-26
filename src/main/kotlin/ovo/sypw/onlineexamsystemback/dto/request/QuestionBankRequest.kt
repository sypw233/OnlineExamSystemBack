package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "创建/更新题库请求")
data class QuestionBankRequest(
    @field:NotBlank(message = "题库名称不能为空")
    @field:Size(max = 100, message = "题库名称长度不能超过100")
    @Schema(
        description = "题库名称",
        example = "Java基础题库",
        required = true,
        maxLength = 100
    )
    val name: String = "",

    @field:Size(max = 500, message = "题库描述长度不能超过500")
    @Schema(
        description = "题库描述",
        example = "包含Java基础知识的所有题目",
        required = false,
        maxLength = 500
    )
    val description: String? = null
)
