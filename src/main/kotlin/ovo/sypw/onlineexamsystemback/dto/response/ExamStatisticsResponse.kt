package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "考试统计响应")
data class ExamStatisticsResponse(
    @Schema(description = "考试ID", example = "1")
    val examId: Long,
    
    @Schema(description = "考试标题", example = "Java期中考试")
    val examTitle: String,
    
    @Schema(description = "考试总人数", example = "50")
    val totalStudents: Int,
    
    @Schema(description = "已提交人数", example = "48")
    val submittedCount: Int,
    
    @Schema(description = "完成率(%)", example = "96.0")
    val completionRate: Double,
    
    @Schema(description = "平均分", example = "75.5")
    val averageScore: Double?,
    
    @Schema(description = "最高分", example = "98")
    val highestScore: Int?,
    
    @Schema(description = "最低分", example = "45")
    val lowestScore: Int?,
    
    @Schema(description = "及格人数", example = "40")
    val passCount: Int,
    
    @Schema(description = "及格率(%)", example = "83.3")
    val passRate: Double,
    
    @Schema(description = "分数段分布")
    val scoreDistribution: Map<String, Int>
)
