package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "考试详情响应")
data class ExamResponse(
    @Schema(description = "考试ID", example = "1")
    val id: Long,
    
    @Schema(description = "考试标题", example = "Java期中考试")
    val title: String,
    
    @Schema(description = "考试描述", example = "Java基础知识测试")
    val description: String?,
    
    @Schema(description = "课程ID", example = "1")
    val courseId: Long,
    
    @Schema(description = "课程名称", example = "Java程序设计")
    val courseName: String,
    
    @Schema(description = "创建者ID", example = "1")
    val creatorId: Long,
    
    @Schema(description = "创建者姓名", example = "张老师")
    val creatorName: String,
    
    @Schema(description = "考试开始时间", example = "2024-12-01T10:00:00")
    val startTime: LocalDateTime,
    
    @Schema(description = "考试结束时间", example = "2024-12-01T12:00:00")
    val endTime: LocalDateTime,
    
    @Schema(description = "考试时长（分钟）", example = "120")
    val duration: Int?,
    
    @Schema(description = "考试总分", example = "100")
    val totalScore: Int,
    
    @Schema(
        description = "考试状态",
        example = "0",
        allowableValues = ["0", "1", "2"]
    )
    val status: Int,
    
    @Schema(description = "状态描述", example = "草稿")
    val statusDescription: String,
    
    @Schema(description = "是否需要手动评分", example = "false")
    val needsGrading: Boolean,
    
    @Schema(description = "题目数量", example = "20")
    val questionCount: Long,
    
    // 监考控制字段
    @Schema(
        description = "允许的考试平台",
        example = "desktop",
        allowableValues = ["desktop", "mobile", "both"]
    )
    val allowedPlatforms: String?,
    
    @Schema(description = "是否开启严格监考", example = "true")
    val strictMode: Boolean,
    
    @Schema(description = "最大允许切出次数", example = "3")
    val maxSwitchCount: Int?,
    
    @Schema(description = "是否要求全屏模式", example = "true")
    val fullscreenRequired: Boolean,
    
    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    val createTime: LocalDateTime
)
