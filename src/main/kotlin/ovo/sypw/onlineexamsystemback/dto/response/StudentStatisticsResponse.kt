package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "学生成绩记录")
data class StudentScoreRecord(
    @Schema(description = "考试ID", example = "1")
    val examId: Long,
    
    @Schema(description = "考试标题", example = "Java期中考试")
    val examTitle: String,
    
    @Schema(description = "考试分数", example = "85")
    val score: Int?,
    
    @Schema(description = "提交时间", example = "2024-10-15T14:30:00")
    val submitTime: LocalDateTime?
)

@Schema(description = "学生统计响应")
data class StudentStatisticsResponse(
    @Schema(description = "学生ID", example = "10")
    val studentId: Long,
    
    @Schema(description = "学生姓名", example = "张三")
    val studentName: String,
    
    @Schema(description = "参加考试总数", example = "8")
    val totalExams: Int,
    
    @Schema(description = "平均成绩", example = "78.5")
    val averageScore: Double?,
    
    @Schema(description = "最高分", example = "95")
    val highestScore: Int?,
    
    @Schema(description = "最低分", example = "62")
    val lowestScore: Int?,
    
    @Schema(description = "成绩记录列表")
    val scores: List<StudentScoreRecord>
)
