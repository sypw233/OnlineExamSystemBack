package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "AI辅助判题请求")
data class AiGradingRequest(
    @field:NotNull(message = "题目ID不能为空")
    @Schema(
        description = "题目ID",
        example = "1",
        required = true
    )
    val questionId: Long? = null,
    
    @field:NotNull(message = "学生答案不能为空")
    @Schema(
        description = "学生答案",
        example = "多态是指同一个方法可以有多种形式...",
        required = true
    )
    val studentAnswer: String? = null,
    
    @field:NotNull(message = "题目满分不能为空")
    @Schema(
        description = "题目满分（该题在考试中的分值）",
        example = "10",
        required = true
    )
    val maxScore: Int? = null
)

@Schema(description = "AI配置更新请求")
data class AiConfigRequest(
    @field:NotNull(message = "配置键不能为空")
    @Schema(
        description = "配置键",
        example = "system_prompt",
        required = true
    )
    val configKey: String? = null,
    
    @field:NotNull(message = "配置值不能为空")
    @Schema(
        description = "配置值",
        example = "你是一个专业的教师助手...",
        required = true
    )
    val configValue: String? = null
)
