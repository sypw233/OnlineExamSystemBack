package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "题目统计响应")
data class QuestionStatisticsResponse(
    @Schema(description = "题目ID", example = "1")
    val questionId: Long,
    
    @Schema(description = "题目内容", example = "Java中的多态是指？")
    val questionContent: String,
    
    @Schema(description = "题目类型", example = "single")
    val questionType: String,
    
    @Schema(description = "使用次数", example = "5")
    val usageCount: Int,
    
    @Schema(description = "总答题次数", example = "250")
    val totalAttempts: Int,
    
    @Schema(description = "答对次数", example = "180")
    val correctCount: Int,
    
    @Schema(description = "正确率(%)", example = "72.0")
    val accuracy: Double,
    
    @Schema(description = "选项分布（仅客观题）")
    val optionDistribution: Map<String, Int>?
)
