package ovo.sypw.onlineexamsystemback.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "批量删除请求")
data class BatchDeleteRequest(
    @field:NotEmpty(message = "ID列表不能为空")
    @Schema(description = "待删除的记录ID列表", example = "[1, 2, 3]")
    val ids: List<Long>
)
