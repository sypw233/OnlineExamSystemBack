package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "成绩导出字段配置")
data class ExamScoreExportRequest(
    @Schema(description = "是否导出学号", example = "true")
    val includeStudentId: Boolean = true,

    @Schema(description = "是否导出姓名", example = "true")
    val includeStudentName: Boolean = true,

    @Schema(description = "是否导出得分", example = "true")
    val includeScore: Boolean = true,

    @Schema(description = "是否导出提交时间", example = "true")
    val includeSubmitTime: Boolean = true,

    @Schema(description = "是否导出状态", example = "true")
    val includeStatus: Boolean = true,

    @Schema(description = "是否导出切出次数", example = "false")
    val includeSwitchCount: Boolean = false,

    @Schema(description = "是否导出监考异常标记", example = "false")
    val includeProctoringAbnormal: Boolean = false
)
