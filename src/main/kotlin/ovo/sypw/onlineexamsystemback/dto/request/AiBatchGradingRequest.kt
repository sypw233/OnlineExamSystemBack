package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "AI批量评分请求")
data class AiBatchGradingRequest(
    @field:NotNull(message = "提交ID不能为空")
    @Schema(description = "考试提交记录ID", example = "1")
    val submissionId: Long? = null,

    @Schema(description = "并行调用数量（默认从系统配置读取，范围1-10）", example = "5")
    val concurrency: Int? = null
)
