package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "监考数据详情响应")
data class ProctoringDataResponse(
    @Schema(description = "提交ID", example = "1")
    val submissionId: Long,

    @Schema(description = "考试ID", example = "1")
    val examId: Long,

    @Schema(description = "用户ID", example = "1")
    val userId: Long,

    @Schema(description = "切出次数", example = "3")
    val switchCount: Int,

    @Schema(description = "详细监考事件数据")
    val proctoringData: Map<String, Any>,

    @Schema(description = "提交状态", example = "1")
    val status: Int
)
