package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "AI辅助判题响应")
data class AiGradingResponse(
    @Schema(description = "题目ID", example = "1")
    val questionId: Long,
    
    @Schema(description = "题目满分", example = "10")
    val maxScore: Int,
    
    @Schema(description = "建议分数", example = "7")
    val suggestedScore: Int,
    
    @Schema(description = "评分说明", example = "学生理解了多态的基本概念，但缺少具体应用场景的描述...")
    val explanation: String,
    
    @Schema(description = "优点列表")
    val strengths: List<String>,
    
    @Schema(description = "改进建议列表")
    val improvements: List<String>
)

@Schema(description = "AI配置响应")
data class AiConfigResponse(
    @Schema(description = "配置ID", example = "1")
    val id: Long,
    
    @Schema(description = "配置键", example = "system_prompt")
    val configKey: String,
    
    @Schema(description = "配置值", example = "你是一个专业的教师助手...")
    val configValue: String?,
    
    @Schema(description = "配置描述", example = "AI判题系统提示词")
    val description: String?
)
