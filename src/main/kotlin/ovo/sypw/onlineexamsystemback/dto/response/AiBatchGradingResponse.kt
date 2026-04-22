package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "AI单题评分详情")
data class AiGradingDetail(
    @Schema(description = "题目ID", example = "1")
    val questionId: Long,

    @Schema(description = "题目内容", example = "请简述TCP和UDP的区别")
    val questionContent: String,

    @Schema(description = "建议分数", example = "8")
    val suggestedScore: Int,

    @Schema(description = "题目满分", example = "10")
    val maxScore: Int,

    @Schema(description = "评分说明", example = "学生准确描述了TCP的可靠性和UDP的高效性...")
    val explanation: String,

    @Schema(description = "优点列表")
    val strengths: List<String>,

    @Schema(description = "改进建议列表")
    val improvements: List<String>
)

@Schema(description = "AI批量评分响应")
data class AiBatchGradingResponse(
    @Schema(description = "提交ID", example = "1")
    val submissionId: Long,

    @Schema(description = "评分题目数量", example = "3")
    val gradedCount: Int,

    @Schema(description = "AI建议总分", example = "75")
    val totalSuggestedScore: Int,

    @Schema(description = "客观题已得分数", example = "45")
    val objectiveScore: Int? = null,

    @Schema(description = "各题评分详情")
    val details: List<AiGradingDetail>
)
