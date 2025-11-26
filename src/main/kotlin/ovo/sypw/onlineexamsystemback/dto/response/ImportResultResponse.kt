package ovo.sypw.onlineexamsystemback.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "导入错误详情")
data class ImportErrorDetail(
    @Schema(description = "错误行号", example = "3")
    val row: Int,
    
    @Schema(description = "错误原因", example = "题型无效: singlee")
    val reason: String
)

@Schema(description = "题目导入结果响应")
data class ImportResultResponse(
    @Schema(description = "任务ID", example = "uuid-123")
    val taskId: String,
    
    @Schema(description = "总行数", example = "100")
    val totalRows: Int,
    
    @Schema(description = "成功导入数量", example = "95")
    val successCount: Int,
    
    @Schema(description = "失败数量", example = "5")
    val failedCount: Int,
    
    @Schema(description = "错误详情列表")
    val errors: List<ImportErrorDetail>
)
