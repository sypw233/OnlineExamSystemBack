package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "提交记录响应")
data class SubmissionResponse(
    @Schema(description = "提交ID", example = "1")
    val id: Long,
    
    @Schema(description = "考试ID", example = "1")
    val examId: Long,
    
    @Schema(description = "考试标题", example = "Java期中考试")
    val examTitle: String,
    
    @Schema(description = "用户ID", example = "1")
    val userId: Long,
    
    @Schema(description = "用户姓名", example = "张三")
    val userName: String,
    
    @Schema(description = "答案（JSON）", example = "{\"1\": \"A\", \"2\": \"B,C\"}")
    val answers: String?,
    
    @Schema(description = "客观题得分", example = "60")
    val objectiveScore: Int?,
    
    @Schema(description = "主观题得分", example = "30")
    val subjectiveScore: Int?,
    
    @Schema(description = "总分", example = "90")
    val totalScore: Int?,
    
    @Schema(
        description = "状态",
        example = "1",
        allowableValues = ["0", "1", "2"]
    )
    val status: Int,
    
    @Schema(description = "状态描述", example = "已提交")
    val statusDescription: String,
    
    @Schema(description = "切出次数", example = "2")
    val switchCount: Int,
    
    @Schema(description = "开始时间", example = "2024-12-01 10:00:00")
    val startTime: LocalDateTime,
    
    @Schema(description = "提交时间", example = "2024-12-01 11:30:00")
    val submitTime: LocalDateTime?,
    
    @Schema(description = "评分详情（JSON）")
    val submitDetail: String?
)
