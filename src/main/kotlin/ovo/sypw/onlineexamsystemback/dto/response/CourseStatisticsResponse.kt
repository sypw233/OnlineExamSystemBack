package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "课程统计响应")
data class CourseStatisticsResponse(
    @Schema(description = "课程ID", example = "1")
    val courseId: Long,
    
    @Schema(description = "课程名称", example = "Java程序设计")
    val courseName: String,
    
    @Schema(description = "学生总数", example = "100")
    val totalStudents: Int,
    
    @Schema(description = "考试总数", example = "5")
    val totalExams: Int,
    
    @Schema(description = "平均成绩", example = "78.5")
    val averageScore: Double?,
    
    @Schema(description = "最高分", example = "98")
    val highestScore: Int?,
    
    @Schema(description = "最低分", example = "45")
    val lowestScore: Int?
)
