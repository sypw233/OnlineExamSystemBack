package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.time.LocalDateTime

@Schema(description = "创建/更新考试请求")
data class ExamRequest(
    @field:NotBlank(message = "考试标题不能为空")
    @field:Size(max = 100, message = "考试标题长度不能超过100")
    @Schema(description = "考试标题", example = "Java期中考试", required = true)
    val title: String = "",

    @field:Size(max = 500, message = "考试描述长度不能超过500")
    @Schema(description = "考试描述", example = "Java基础知识测试", required = false)
    val description: String? = null,

    @field:NotNull(message = "课程ID不能为空")
    @Schema(description = "所属课程ID", example = "1", required = true)
    val courseId: Long? = null,

    @field:NotNull(message = "开始时间不能为空")
    @Schema(description = "考试开始时间", example = "2024-12-01T10:00:00", required = true)
    val startTime: LocalDateTime? = null,

    @field:NotNull(message = "结束时间不能为空")
    @Schema(description = "考试结束时间", example = "2024-12-01T12:00:00", required = true)
    val endTime: LocalDateTime? = null,

    @field:Min(value = 1, message = "考试时长最少1分钟")
    @Schema(description = "考试时长（分钟）", example = "120", required = false)
    val duration: Int? = null,

    @field:Min(value = 1, message = "总分最少1分")
    @Schema(description = "考试总分", example = "100", required = false)
    val totalScore: Int = 100,

    @Schema(description = "是否需要手动评分", example = "false", required = false)
    val needsGrading: Boolean = false,

    // 监考控制字段
    @field:Pattern(
        regexp = "^(desktop|mobile|both)$",
        message = "平台限制必须是: desktop, mobile, both"
    )
    @Schema(
        description = "允许的考试平台",
        example = "desktop",
        allowableValues = ["desktop", "mobile", "both"],
        required = false
    )
    val allowedPlatforms: String? = "both",

    @Schema(description = "是否开启严格监考", example = "true", required = false)
    val strictMode: Boolean = false,

    @field:Min(value = 0, message = "最大切出次数不能为负数")
    @Schema(
        description = "最大允许切出次数（null表示无限制）",
        example = "3",
        required = false
    )
    val maxSwitchCount: Int? = null,

    @Schema(description = "是否要求全屏模式（仅桌面端）", example = "true", required = false)
    val fullscreenRequired: Boolean = false
)
