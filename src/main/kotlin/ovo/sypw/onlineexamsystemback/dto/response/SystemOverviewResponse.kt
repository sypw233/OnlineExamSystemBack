package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "系统总览响应")
data class SystemOverviewResponse(
    @Schema(description = "用户总数", example = "500")
    val totalUsers: Int,
    
    @Schema(description = "学生数", example = "400")
    val studentCount: Int,
    
    @Schema(description = "教师数", example = "50")
    val teacherCount: Int,
    
    @Schema(description = "管理员数", example = "5")
    val adminCount: Int,
    
    @Schema(description = "课程总数", example = "20")
    val totalCourses: Int,
    
    @Schema(description = "考试总数", example = "100")
    val totalExams: Int,
    
    @Schema(description = "题库总题数", example = "5000")
    val totalQuestions: Int,
    
    @Schema(description = "提交总数", example = "8000")
    val totalSubmissions: Int
)
