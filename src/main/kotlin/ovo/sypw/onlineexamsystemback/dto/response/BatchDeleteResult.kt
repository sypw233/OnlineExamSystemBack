package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "批量删除失败详情")
data class FailedDetail(
    @Schema(description = "失败的记录ID", example = "1")
    val id: Long,

    @Schema(description = "失败原因", example = "该题目已被使用，无法删除")
    val reason: String
)

@Schema(description = "批量删除结果")
data class BatchDeleteResult(
    @Schema(description = "成功删除数量", example = "3")
    val successCount: Int,

    @Schema(description = "失败数量", example = "1")
    val failedCount: Int,

    @Schema(description = "成功删除的ID列表")
    val successIds: List<Long>,

    @Schema(description = "失败详情列表")
    val failedDetails: List<FailedDetail>
)
