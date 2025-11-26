package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = "添加题目到考试请求")
data class ExamQuestionRequest(
    @field:NotNull(message = "题目ID不能为空")
    @Schema(description = "题目ID", example = "1", required = true)
    val questionId: Long? = null,

    @field:NotNull(message = "题目分值不能为空")
    @field:Min(value = 1, message = "题目分值最少1分")
    @Schema(description = "题目分值", example = "10", required = true)
    val score: Int? = null,

    @field:NotNull(message = "题目序号不能为空")
    @field:Min(value = 1, message = "题目序号从1开始")
    @Schema(description = "题目显示顺序", example = "1", required = true)
    val sequence: Int? = null
)
